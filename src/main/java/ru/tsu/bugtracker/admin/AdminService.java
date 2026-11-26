package ru.tsu.bugtracker.admin;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.admin.dto.AdminRoleRequest;
import ru.tsu.bugtracker.admin.dto.AdminUserDetailsDto;
import ru.tsu.bugtracker.admin.dto.AdminUserSummaryDto;
import ru.tsu.bugtracker.admin.dto.BlockUserRequest;
import ru.tsu.bugtracker.admin.dto.DeactivateUserRequest;
import ru.tsu.bugtracker.admin.dto.UserProjectMembershipDto;
import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.auth.PasswordResetToken;
import ru.tsu.bugtracker.auth.PasswordResetTokenRepository;
import ru.tsu.bugtracker.auth.RefreshTokenRepository;
import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.common.util.TokenHashUtil;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.project.InvitationStatus;
import ru.tsu.bugtracker.project.MemberStatus;
import ru.tsu.bugtracker.project.ProjectInvitation;
import ru.tsu.bugtracker.project.ProjectInvitationRepository;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectMemberRepository;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

/**
 * Administrative operations on user accounts.
 */
@Service
@Transactional
public class AdminService {

	private static final String TARGET_TYPE_USER = "USER";

	private static final int PASSWORD_RESET_TOKEN_TTL_HOURS = 1;

	private final UserRepository userRepository;

	private final ProjectMemberRepository projectMemberRepository;

	private final ProjectInvitationRepository projectInvitationRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final PasswordResetTokenRepository passwordResetTokenRepository;

	private final AuditService auditService;

	private final MailService mailService;

	public AdminService(
			UserRepository userRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectInvitationRepository projectInvitationRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			AuditService auditService,
			MailService mailService) {
		this.userRepository = userRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectInvitationRepository = projectInvitationRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.auditService = auditService;
		this.mailService = mailService;
	}

	/**
	 * Returns a filtered, paginated list of users.
	 *
	 * @param search   case-insensitive fragment of the email or full name, or {@code null}
	 * @param status   required account status, or {@code null}
	 * @param isAdmin  required administrator flag, or {@code null}
	 * @param pageable pagination and sorting
	 * @return page of user summaries
	 */
	@Transactional(readOnly = true)
	public PagedResponse<AdminUserSummaryDto> listUsers(String search, UserStatus status, Boolean isAdmin,
			Pageable pageable) {
		Specification<User> specification = (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase() + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern)));
			}
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (isAdmin != null) {
				predicates.add(criteriaBuilder.equal(root.get("admin"), isAdmin));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};

		Page<AdminUserSummaryDto> page = userRepository.findAll(specification, pageable)
				.map(this::toSummary);
		return PagedResponse.from(page);
	}

	/**
	 * Returns the full administrative view of a user.
	 *
	 * @param userId user identifier
	 * @return user details
	 */
	@Transactional(readOnly = true)
	public AdminUserDetailsDto getUser(UUID userId) {
		User user = requireUser(userId);
		List<UserProjectMembershipDto> projects = projectMemberRepository.findAllByUserId(userId).stream()
				.map(UserProjectMembershipDto::from)
				.toList();
		return toDetails(user, projects);
	}

	/**
	 * Blocks a user account and revokes all of its refresh tokens.
	 *
	 * @param actorId      identifier of the administrator performing the action
	 * @param targetUserId identifier of the user to block
	 * @param request      block request carrying the reason
	 * @return updated user details
	 */
	public AdminUserDetailsDto blockUser(UUID actorId, UUID targetUserId, BlockUserRequest request) {
		User target = requireUser(targetUserId);
		if (actorId != null && actorId.equals(targetUserId)) {
			throw new ConflictException("You cannot block your own account");
		}
		if (target.getStatus() == UserStatus.DEACTIVATED) {
			throw new ConflictException("A deactivated account cannot be blocked");
		}

		target.setStatus(UserStatus.BLOCKED);
		refreshTokenRepository.revokeAllActiveByUserId(targetUserId, OffsetDateTime.now(ZoneOffset.UTC));

		Map<String, Object> details = new LinkedHashMap<>();
		if (request != null && request.reason() != null && !request.reason().isBlank()) {
			details.put("reason", request.reason());
		}
		audit(actorId, "USER_BLOCKED", targetUserId, details);

		return getUser(targetUserId);
	}

	/**
	 * Unblocks a previously blocked user account.
	 *
	 * @param actorId      identifier of the administrator performing the action
	 * @param targetUserId identifier of the user to unblock
	 * @return updated user details
	 */
	public AdminUserDetailsDto unblockUser(UUID actorId, UUID targetUserId) {
		User target = requireUser(targetUserId);
		if (target.getStatus() == UserStatus.DEACTIVATED) {
			throw new ConflictException("A deactivated account cannot be unblocked");
		}

		target.setStatus(UserStatus.ACTIVE);
		audit(actorId, "USER_UNBLOCKED", targetUserId, Map.of());

		return getUser(targetUserId);
	}

	/**
	 * Issues a password reset token for a user and sends the reset email.
	 *
	 * @param actorId      identifier of the administrator performing the action
	 * @param targetUserId identifier of the user whose password should be reset
	 * @return confirmation message
	 */
	public MessageResponse triggerPasswordReset(UUID actorId, UUID targetUserId) {
		User target = requireUser(targetUserId);

		String rawToken = TokenHashUtil.generateRawToken();
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		PasswordResetToken token = new PasswordResetToken();
		token.setUser(target);
		token.setTokenHash(TokenHashUtil.sha256Hex(rawToken));
		token.setCreatedAt(now);
		token.setExpiresAt(now.plusHours(PASSWORD_RESET_TOKEN_TTL_HOURS));
		passwordResetTokenRepository.save(token);

		mailService.sendPasswordResetEmail(target.getEmail(), target.getFullName(), rawToken);
		audit(actorId, "ADMIN_PASSWORD_RESET_TRIGGERED", targetUserId, Map.of());

		return new MessageResponse("Password reset email has been sent");
	}

	/**
	 * Verifies a user's email address and applies their pending project invitations.
	 *
	 * @param actorId      identifier of the administrator performing the action
	 * @param targetUserId identifier of the user to verify
	 * @return updated user details
	 */
	public AdminUserDetailsDto verifyUserEmail(UUID actorId, UUID targetUserId) {
		User target = requireUser(targetUserId);
		target.setEmailVerified(true);

		List<ProjectInvitation> invitations = projectInvitationRepository
				.findAllByEmailIgnoreCaseAndStatus(target.getEmail(), InvitationStatus.PENDING);
		Instant now = Instant.now();
		for (ProjectInvitation invitation : invitations) {
			if (invitation.getExpiresAt() != null && !invitation.getExpiresAt().isAfter(now)) {
				continue;
			}
			acceptInvitation(target, invitation);
		}

		audit(actorId, "ADMIN_EMAIL_VERIFIED", targetUserId, Map.of());

		return getUser(targetUserId);
	}

	/**
	 * Grants or revokes the administrator role.
	 *
	 * @param actorId      identifier of the administrator performing the action
	 * @param targetUserId identifier of the user whose role should be changed
	 * @param request      requested administrator flag
	 * @return updated user details
	 */
	public AdminUserDetailsDto updateAdminRole(UUID actorId, UUID targetUserId, AdminRoleRequest request) {
		User target = requireUser(targetUserId);
		boolean newAdmin = Boolean.TRUE.equals(request.isAdmin());

		if (!newAdmin && target.isAdmin() && countActiveAdmins() <= 1) {
			throw new ConflictException("Cannot remove the last administrator");
		}

		target.setAdmin(newAdmin);
		audit(actorId, "ADMIN_ROLE_UPDATED", targetUserId, Map.of("isAdmin", newAdmin));

		return getUser(targetUserId);
	}

	/**
	 * Deactivates a user account and revokes all of its refresh tokens.
	 *
	 * @param actorId      identifier of the administrator performing the action
	 * @param targetUserId identifier of the user to deactivate
	 * @param request      deactivation request carrying the reason
	 * @return updated user details
	 */
	public AdminUserDetailsDto deactivateUser(UUID actorId, UUID targetUserId, DeactivateUserRequest request) {
		User target = requireUser(targetUserId);
		if (actorId != null && actorId.equals(targetUserId)) {
			throw new ConflictException("You cannot deactivate your own account");
		}
		if (target.isAdmin() && countActiveAdmins() <= 1) {
			throw new ConflictException("Cannot deactivate the last administrator");
		}

		target.setStatus(UserStatus.DEACTIVATED);
		refreshTokenRepository.revokeAllActiveByUserId(targetUserId, OffsetDateTime.now(ZoneOffset.UTC));

		Map<String, Object> details = new LinkedHashMap<>();
		if (request != null && request.reason() != null && !request.reason().isBlank()) {
			details.put("reason", request.reason());
		}
		audit(actorId, "USER_DEACTIVATED", targetUserId, details);

		return getUser(targetUserId);
	}

	private User requireUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found"));
	}

	private long countActiveAdmins() {
		return userRepository.countByAdminTrueAndStatusNot(UserStatus.DEACTIVATED);
	}

	private void acceptInvitation(User user, ProjectInvitation invitation) {
		UUID projectId = invitation.getProject().getId();
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
			ProjectMember member = new ProjectMember();
			member.setProject(invitation.getProject());
			member.setUser(user);
			member.setRole(invitation.getRole());
			member.setStatus(MemberStatus.ACTIVE);
			projectMemberRepository.save(member);
		}
		invitation.setStatus(InvitationStatus.ACCEPTED);
	}

	private void audit(UUID actorId, String action, UUID targetId, Map<String, Object> details) {
		User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
		auditService.log(actor, action, TARGET_TYPE_USER, targetId, details);
	}

	private AdminUserSummaryDto toSummary(User user) {
		int projectsCount = projectMemberRepository.findAllByUserId(user.getId()).size();
		return new AdminUserSummaryDto(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.isAdmin(),
				user.isEmailVerified(),
				user.getStatus(),
				projectsCount,
				user.getCreatedAt());
	}

	private AdminUserDetailsDto toDetails(User user, List<UserProjectMembershipDto> projects) {
		return new AdminUserDetailsDto(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getAvatarUrl(),
				user.isAdmin(),
				user.isEmailVerified(),
				user.getStatus(),
				user.isCanCreateProjects(),
				null,
				projects,
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

}
