package ru.tsu.bugtracker.project;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.common.exception.UnprocessableEntityException;
import ru.tsu.bugtracker.common.util.TokenHashUtil;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.project.dto.AddMemberResultDto;
import ru.tsu.bugtracker.project.dto.AddProjectMemberRequest;
import ru.tsu.bugtracker.project.dto.InvitationDetailsDto;
import ru.tsu.bugtracker.project.dto.ProjectInvitationDto;
import ru.tsu.bugtracker.project.dto.ProjectMemberDto;
import ru.tsu.bugtracker.project.dto.ProjectTeamResponseDto;
import ru.tsu.bugtracker.project.dto.TeamFinalizeResponseDto;
import ru.tsu.bugtracker.project.dto.UpdateMemberRoleRequest;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

@Service
@Transactional
public class ProjectTeamService {

	private static final long INVITATION_TTL_DAYS = 7L;

	private static final String PROJECT_MEMBER_NOT_FOUND = "Project member not found";

	private static final String PROJECT_INVITATION_NOT_FOUND = "Project invitation not found";

	private final ProjectAccessService projectAccessService;

	private final ProjectMemberRepository projectMemberRepository;

	private final ProjectInvitationRepository projectInvitationRepository;

	private final UserRepository userRepository;

	private final MailService mailService;

	public ProjectTeamService(ProjectAccessService projectAccessService,
			ProjectMemberRepository projectMemberRepository,
			ProjectInvitationRepository projectInvitationRepository,
			UserRepository userRepository,
			MailService mailService) {
		this.projectAccessService = projectAccessService;
		this.projectMemberRepository = projectMemberRepository;
		this.projectInvitationRepository = projectInvitationRepository;
		this.userRepository = userRepository;
		this.mailService = mailService;
	}

	public ProjectTeamResponseDto listTeam(UUID projectId, UUID userId) {
		this.projectAccessService.requireMember(projectId, userId);
		Project project = this.projectAccessService.requireProject(projectId);

		List<ProjectMember> memberEntities = this.projectMemberRepository
			.findAllByProjectIdOrderByJoinedAtAsc(projectId);

		List<ProjectMemberDto> members = memberEntities.stream()
			.map(ProjectMemberDto::from)
			.toList();

		List<ProjectInvitationDto> invitations = this.projectInvitationRepository
			.findAllByProjectIdAndStatusOrderByCreatedAtDesc(projectId, InvitationStatus.PENDING)
			.stream()
			.map(ProjectInvitationDto::from)
			.toList();

		boolean hasTeamLead = memberEntities.stream()
			.anyMatch(member -> member.getRole() == ProjectRole.TEAM_LEAD
					&& member.getStatus() == MemberStatus.ACTIVE);

		boolean hasActiveTester = memberEntities.stream()
			.anyMatch(member -> member.getRole() == ProjectRole.TESTER
					&& member.getStatus() == MemberStatus.ACTIVE);

		return new ProjectTeamResponseDto(members, invitations, hasTeamLead, hasActiveTester,
				project.isBugTrackingEnabled());
	}

	public AddMemberResultDto addMember(UUID projectId, UUID userId, AddProjectMemberRequest req) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		ProjectMember inviter = this.projectAccessService.requireRole(projectId, userId,
				ProjectRole.TEAM_LEAD);

		String email = req.email();
		ProjectRole role = req.role();
		User user = this.userRepository.findByEmail(email).orElse(null);

		if (user != null) {
			if (this.projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
				throw new ConflictException("User is already a member of the project");
			}
			if (user.getStatus() != UserStatus.ACTIVE) {
				throw new ConflictException("User account is not active");
			}

			ProjectMember member = new ProjectMember();
			member.setProject(project);
			member.setUser(user);
			member.setRole(role);
			member.setStatus(MemberStatus.ACTIVE);
			member.setJoinedAt(OffsetDateTime.now());
			ProjectMember saved = this.projectMemberRepository.save(member);

			this.projectAccessService.recalculateBugTrackingEnabled(project);

			return new AddMemberResultDto("MEMBER_ADDED", ProjectMemberDto.from(saved), null);
		}

		if (this.projectInvitationRepository.existsByProjectIdAndEmailIgnoreCaseAndStatus(
				projectId, email, InvitationStatus.PENDING)) {
			throw new ConflictException("Invitation for this email is already pending");
		}

		Instant now = Instant.now();
		String rawToken = TokenHashUtil.generateToken();

		ProjectInvitation invitation = new ProjectInvitation();
		invitation.setProject(project);
		invitation.setEmail(email);
		invitation.setRole(role);
		invitation.setTokenHash(TokenHashUtil.sha256Hex(rawToken));
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setInvitedBy(inviter.getUser());
		invitation.setCreatedAt(now);
		invitation.setExpiresAt(now.plus(INVITATION_TTL_DAYS, ChronoUnit.DAYS));
		ProjectInvitation saved = this.projectInvitationRepository.save(invitation);

		this.mailService.sendProjectInvitationEmail(saved.getEmail(), project.getName(),
				inviter.getUser().getFullName(), rawToken);

		return new AddMemberResultDto("INVITATION_SENT", null, ProjectInvitationDto.from(saved));
	}

	public ProjectMemberDto updateMemberRole(UUID projectId, UUID memberId, UUID userId,
			UpdateMemberRoleRequest req) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		ProjectMember member = this.projectMemberRepository.findByIdAndProjectId(memberId, projectId)
			.orElseThrow(() -> new NotFoundException(PROJECT_MEMBER_NOT_FOUND));

		ProjectRole newRole = req.role();
		boolean demotesLastTeamLead = member.getRole() == ProjectRole.TEAM_LEAD
				&& newRole != ProjectRole.TEAM_LEAD
				&& this.projectMemberRepository.countByProjectIdAndRoleAndStatus(
						projectId, ProjectRole.TEAM_LEAD, MemberStatus.ACTIVE) <= 1;

		if (demotesLastTeamLead) {
			throw new ConflictException("Cannot remove the last team lead of the project");
		}

		member.setRole(newRole);
		ProjectMember saved = this.projectMemberRepository.save(member);

		this.projectAccessService.recalculateBugTrackingEnabled(project);

		return ProjectMemberDto.from(saved);
	}

	public void removeMember(UUID projectId, UUID memberId, UUID userId) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		ProjectMember member = this.projectMemberRepository.findByIdAndProjectId(memberId, projectId)
			.orElseThrow(() -> new NotFoundException(PROJECT_MEMBER_NOT_FOUND));

		if (member.getRole() == ProjectRole.TEAM_LEAD
				&& this.projectMemberRepository.countByProjectIdAndRoleAndStatus(
						projectId, ProjectRole.TEAM_LEAD, MemberStatus.ACTIVE) <= 1) {
			throw new ConflictException("Cannot remove the last team lead of the project");
		}

		this.projectMemberRepository.delete(member);

		this.projectAccessService.recalculateBugTrackingEnabled(project);
	}

	public TeamFinalizeResponseDto finalizeTeam(UUID projectId, UUID userId) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		boolean bugTrackingEnabled = this.projectAccessService
			.recalculateBugTrackingEnabled(project);
		boolean hasActiveTester = this.projectAccessService.hasActiveTester(projectId);

		String warning = bugTrackingEnabled
				? null
				: "Project does not have an active tester; bug creation remains disabled";

		return new TeamFinalizeResponseDto(bugTrackingEnabled, hasActiveTester, warning);
	}

	public List<ProjectInvitationDto> listInvitations(UUID projectId, UUID userId) {
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		return this.projectInvitationRepository
			.findAllByProjectIdAndStatusOrderByCreatedAtDesc(projectId, InvitationStatus.PENDING)
			.stream()
			.map(ProjectInvitationDto::from)
			.toList();
	}

	public void cancelInvitation(UUID projectId, UUID invitationId, UUID userId) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		ProjectInvitation invitation = this.projectInvitationRepository
			.findByIdAndProjectId(invitationId, projectId)
			.orElseThrow(() -> new NotFoundException(PROJECT_INVITATION_NOT_FOUND));

		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new ConflictException("Only pending invitations can be cancelled");
		}

		invitation.setStatus(InvitationStatus.CANCELLED);
		this.projectInvitationRepository.save(invitation);
	}

	public InvitationDetailsDto getInvitationByToken(String rawToken) {
		String tokenHash = TokenHashUtil.sha256Hex(rawToken);
		ProjectInvitation invitation = this.projectInvitationRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new NotFoundException(PROJECT_INVITATION_NOT_FOUND));

		if (invitation.getStatus() == InvitationStatus.PENDING
				&& invitation.getExpiresAt().isBefore(Instant.now())) {
			invitation.setStatus(InvitationStatus.EXPIRED);
			this.projectInvitationRepository.save(invitation);
		}

		Project project = invitation.getProject();
		return new InvitationDetailsDto(project.getId(), project.getName(), invitation.getEmail(),
				invitation.getRole(), invitation.getStatus(), invitation.getExpiresAt());
	}

	public ProjectMemberDto acceptInvitation(String rawToken, UUID userId) {
		String tokenHash = TokenHashUtil.sha256Hex(rawToken);
		ProjectInvitation invitation = this.projectInvitationRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new NotFoundException(PROJECT_INVITATION_NOT_FOUND));

		if (invitation.getStatus() != InvitationStatus.PENDING
				|| invitation.getExpiresAt().isBefore(Instant.now())) {
			throw new UnprocessableEntityException("Invitation is no longer valid");
		}

		Project project = invitation.getProject();
		if (project.getStatus() != ProjectStatus.ACTIVE) {
			throw new ConflictException("Project is not active");
		}

		User user = this.userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("User not found"));

		if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
			throw new ForbiddenException("Invitation was issued for another email");
		}

		if (this.projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
			throw new ConflictException("User is already a member of the project");
		}

		ProjectMember member = new ProjectMember();
		member.setProject(project);
		member.setUser(user);
		member.setRole(invitation.getRole());
		member.setStatus(MemberStatus.ACTIVE);
		member.setJoinedAt(OffsetDateTime.now());
		ProjectMember saved = this.projectMemberRepository.save(member);

		invitation.setStatus(InvitationStatus.ACCEPTED);
		this.projectInvitationRepository.save(invitation);

		this.projectAccessService.recalculateBugTrackingEnabled(project);

		return ProjectMemberDto.from(saved);
	}

}
