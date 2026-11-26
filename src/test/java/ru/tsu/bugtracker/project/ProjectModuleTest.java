package ru.tsu.bugtracker.project;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.project.dto.CreateProjectRequest;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectModuleTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private ProjectInvitationRepository projectInvitationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private BugRepository bugRepository;

	@Mock
	private AuditService auditService;

	@Mock
	private MailService mailService;

	@Mock
	private ProjectAccessService projectAccessService;

	@InjectMocks
	private ProjectService projectService;

	@InjectMocks
	private ProjectTeamService projectTeamService;

	private ProjectAccessService realProjectAccessService;

	@BeforeEach
	void setUp() {
		this.realProjectAccessService = new ProjectAccessService(this.projectRepository,
				this.projectMemberRepository);
	}

	@Test
	void recalculateBugTrackingEnabledEnablesFlagWhenActiveTesterExists() {
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId);
		when(this.projectMemberRepository.countByProjectIdAndRoleAndStatus(projectId, ProjectRole.TESTER,
				MemberStatus.ACTIVE))
			.thenReturn(1L);

		assertTrue(this.realProjectAccessService.recalculateBugTrackingEnabled(project));
		assertTrue(project.isBugTrackingEnabled());
	}

	@Test
	void recalculateBugTrackingEnabledDisablesFlagWithoutActiveTester() {
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId);
		when(this.projectMemberRepository.countByProjectIdAndRoleAndStatus(projectId, ProjectRole.TESTER,
				MemberStatus.ACTIVE))
			.thenReturn(0L);

		assertFalse(this.realProjectAccessService.recalculateBugTrackingEnabled(project));
		assertFalse(project.isBugTrackingEnabled());
	}

	@Test
	void requireActiveProjectThrowsConflictForArchivedProject() {
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId);
		project.setStatus(ProjectStatus.ARCHIVED);
		when(this.projectRepository.findById(projectId)).thenReturn(Optional.of(project));

		assertThrows(ConflictException.class,
				() -> this.realProjectAccessService.requireActiveProject(projectId));
	}

	@Test
	void createProjectThrowsForbiddenWhenUserCannotCreateProjects() {
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setStatus(UserStatus.ACTIVE);
		user.setCanCreateProjects(false);
		when(this.userRepository.findById(userId)).thenReturn(Optional.of(user));

		CreateProjectRequest request = new CreateProjectRequest("Project", null, null, null, null, null);

		assertThrows(ForbiddenException.class, () -> this.projectService.createProject(userId, request));
	}

	@Test
	void removeMemberThrowsConflictWhenRemovingLastTeamLead() {
		UUID projectId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(this.projectAccessService.requireActiveProject(projectId)).thenReturn(project(projectId));

		ProjectMember teamLead = new ProjectMember();
		teamLead.setRole(ProjectRole.TEAM_LEAD);
		when(this.projectMemberRepository.findByIdAndProjectId(memberId, projectId))
			.thenReturn(Optional.of(teamLead));
		when(this.projectMemberRepository.countByProjectIdAndRoleAndStatus(projectId, ProjectRole.TEAM_LEAD,
				MemberStatus.ACTIVE))
			.thenReturn(1L);

		assertThrows(ConflictException.class,
				() -> this.projectTeamService.removeMember(projectId, memberId, userId));
	}

	private static Project project(UUID id) {
		Project project = new Project();
		project.setId(id);
		project.setStatus(ProjectStatus.ACTIVE);
		return project;
	}

}
