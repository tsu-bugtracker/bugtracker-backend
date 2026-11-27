package ru.tsu.bugtracker.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugHistoryRepository;
import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.bug.BugService;
import ru.tsu.bugtracker.bug.BugStatus;
import ru.tsu.bugtracker.bug.BugSupportService;
import ru.tsu.bugtracker.bug.dto.CreateBugRequest;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.planning.dto.FixPlanRequest;
import ru.tsu.bugtracker.project.LabelRepository;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectMemberRepository;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.project.ProjectStatus;
import ru.tsu.bugtracker.project.SoftwareComponentRepository;
import ru.tsu.bugtracker.release.ReleaseRepository;
import ru.tsu.bugtracker.user.User;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class PlanningAndBugServiceTest {

	private static final UUID PROJECT_ID = UUID.randomUUID();
	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID BUG_ID = UUID.randomUUID();

	@Mock
	private BugSupportService bugSupportService;

	@Mock
	private BugRepository bugRepository;

	@Mock
	private BugHistoryRepository bugHistoryRepository;

	@Mock
	private ProjectAccessService projectAccessService;

	@Mock
	private SoftwareComponentRepository softwareComponentRepository;

	@Mock
	private LabelRepository labelRepository;

	@Mock
	private FixPlanRepository fixPlanRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private ReleaseRepository releaseRepository;

	@InjectMocks
	private BugService bugService;

	@InjectMocks
	private PlanningService planningService;

	@Test
	void createBug_throwsConflict_whenBugTrackingDisabled() {
		Project project = project(false);
		ProjectMember member = member(project, ProjectRole.TESTER);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(PROJECT_ID, USER_ID, ProjectRole.TESTER))
				.thenReturn(member);

		CreateBugRequest request = new CreateBugRequest(
				"Broken export",
				"Export produces an empty file",
				"Open a project and press Export",
				"Chrome 120",
				"CSV file with rows",
				"Empty file",
				BugPriority.MEDIUM,
				null,
				List.of(),
				List.of());

		assertThrows(ConflictException.class,
				() -> bugService.createBug(PROJECT_ID, USER_ID, request));

		verify(bugRepository, never()).save(any(Bug.class));
	}

	@Test
	void planBugFix_throwsBadRequest_whenPriorityChangedWithoutReason() {
		Bug bug = bug(BugStatus.NEW, null);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		stubPlanningAccess(ProjectRole.TEAM_LEAD);

		FixPlanRequest request = new FixPlanRequest(
				BugPriority.HIGH, null, null, null, null, "Needs a hotfix");

		assertThrows(BadRequestException.class,
				() -> planningService.planBugFix(BUG_ID, USER_ID, request));
	}

	@Test
	void planBugFix_throwsConflict_whenBugClosed() {
		Bug bug = bug(BugStatus.CLOSED, null);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		stubPlanningAccess(ProjectRole.TEAM_LEAD);

		FixPlanRequest request = new FixPlanRequest(
				BugPriority.MEDIUM, null, null, null, null, "Reopen and fix");

		assertThrows(ConflictException.class,
				() -> planningService.planBugFix(BUG_ID, USER_ID, request));
	}

	@Test
	void takeBugInWork_rules() {
		User user = user(USER_ID);
		when(bugSupportService.requireUser(USER_ID)).thenReturn(user);
		stubPlanningAccess(ProjectRole.DEVELOPER);

		Bug newBug = bug(BugStatus.PLANNED, null);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(newBug);
		planningService.takeBugInWork(BUG_ID, USER_ID);
		assertEquals(BugStatus.IN_PROGRESS, newBug.getStatus());
		assertEquals(USER_ID, newBug.getAssignee().getId());

		User otherUser = user(UUID.randomUUID());
		Bug assignedBug = bug(BugStatus.PLANNED, otherUser);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(assignedBug);
		assertThrows(ConflictException.class,
				() -> planningService.takeBugInWork(BUG_ID, USER_ID));

		Bug closedBug = bug(BugStatus.CLOSED, null);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(closedBug);
		assertThrows(ConflictException.class,
				() -> planningService.takeBugInWork(BUG_ID, USER_ID));
	}

	private void stubPlanningAccess(ProjectRole role) {
		Project project = project(true);
		ProjectMember member = member(project, role);
		lenient().when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		lenient().when(projectAccessService.requireRole(PROJECT_ID, USER_ID, role)).thenReturn(member);
		lenient().when(projectAccessService.requireMember(PROJECT_ID, USER_ID)).thenReturn(member);
	}

	private static Project project(boolean bugTrackingEnabled) {
		Project project = new Project();
		project.setId(PROJECT_ID);
		project.setStatus(ProjectStatus.ACTIVE);
		project.setBugTrackingEnabled(bugTrackingEnabled);
		return project;
	}

	private static User user(UUID id) {
		User user = new User();
		user.setId(id);
		return user;
	}

	private static ProjectMember member(Project project, ProjectRole role) {
		ProjectMember member = new ProjectMember();
		member.setProject(project);
		member.setUser(user(USER_ID));
		member.setRole(role);
		return member;
	}

	private static Bug bug(BugStatus status, User assignee) {
		Bug bug = new Bug();
		bug.setId(BUG_ID);
		bug.setProject(project(true));
		bug.setStatus(status);
		bug.setPriority(BugPriority.MEDIUM);
		bug.setRecommendedPriority(BugPriority.MEDIUM);
		bug.setAssignee(assignee);
		return bug;
	}
}
