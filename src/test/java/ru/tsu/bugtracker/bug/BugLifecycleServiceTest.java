package ru.tsu.bugtracker.bug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.tsu.bugtracker.bug.dto.ClarifyBugRequest;
import ru.tsu.bugtracker.bug.dto.CloseWithoutFixRequest;
import ru.tsu.bugtracker.bug.dto.FixBugRequest;
import ru.tsu.bugtracker.bug.dto.ReopenBugRequest;
import ru.tsu.bugtracker.bug.dto.RequestClarificationRequest;
import ru.tsu.bugtracker.bug.dto.VerifyBugRequest;
import ru.tsu.bugtracker.bug.dto.VerifyDecision;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.user.User;

@ExtendWith(MockitoExtension.class)
class BugLifecycleServiceTest {

	private static final UUID BUG_ID = UUID.randomUUID();
	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID PROJECT_ID = UUID.randomUUID();

	@Mock
	private BugSupportService bugSupportService;

	@Mock
	private BugRepository bugRepository;

	@Mock
	private ProjectAccessService projectAccessService;

	@InjectMocks
	private BugLifecycleService bugLifecycleService;

	private Bug bug;
	private User actor;

	@BeforeEach
	void setUp() {
		Project project = new Project();
		project.setId(PROJECT_ID);

		bug = new Bug();
		bug.setId(BUG_ID);
		bug.setProject(project);

		actor = new User();
		actor.setId(USER_ID);

		ProjectMember membership = new ProjectMember();
		membership.setProject(project);
		membership.setUser(actor);

		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(eq(PROJECT_ID), eq(USER_ID), any(ProjectRole[].class)))
			.thenReturn(membership);
		lenient().when(bugRepository.save(any(Bug.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void requestClarification_movesInProgressToNeedsInfo() {
		bug.setStatus(BugStatus.IN_PROGRESS);

		bugLifecycleService.requestClarification(
				BUG_ID, USER_ID, new RequestClarificationRequest("Which browser are you using?"));

		assertEquals(BugStatus.NEEDS_INFO, bug.getStatus());
		verify(bugSupportService).recordHistory(eq(bug), eq(actor), eq("status"), any(), any());
		verify(bugSupportService).addSystemComment(eq(bug), eq(actor), anyString());
	}

	@Test
	void requestClarification_throwsConflict_whenNotInProgress() {
		bug.setStatus(BugStatus.NEW);

		assertThrows(
				ConflictException.class,
				() -> bugLifecycleService.requestClarification(
						BUG_ID, USER_ID, new RequestClarificationRequest("Which browser are you using?")));

		verify(bugSupportService, never()).recordHistory(any(), any(), anyString(), any(), any());
	}

	@Test
	void clarifyBug_movesNeedsInfoToInProgress() {
		bug.setStatus(BugStatus.NEEDS_INFO);

		bugLifecycleService.clarifyBug(
				BUG_ID,
				USER_ID,
				new ClarifyBugRequest("Open the page and click submit", "Chrome 120", "Tried again"));

		assertEquals(BugStatus.IN_PROGRESS, bug.getStatus());
	}

	@Test
	void fixBug_movesInProgressAndReturnedToInReview() {
		bug.setStatus(BugStatus.IN_PROGRESS);

		bugLifecycleService.fixBug(BUG_ID, USER_ID, new FixBugRequest(List.of(), "Fixed the null check"));
		assertEquals(BugStatus.IN_REVIEW, bug.getStatus());

		bug.setStatus(BugStatus.RETURNED);

		bugLifecycleService.fixBug(BUG_ID, USER_ID, new FixBugRequest(List.of(), "Fixed again"));
		assertEquals(BugStatus.IN_REVIEW, bug.getStatus());
	}

	@Test
	void verifyAndCloseAndReopen() {
		bug.setStatus(BugStatus.IN_REVIEW);

		bugLifecycleService.verifyBug(
				BUG_ID,
				USER_ID,
				new VerifyBugRequest(VerifyDecision.CONFIRM, null, "Verified on staging"));
		assertEquals(BugStatus.CLOSED, bug.getStatus());

		bug.setStatus(BugStatus.IN_REVIEW);

		bugLifecycleService.verifyBug(
				BUG_ID,
				USER_ID,
				new VerifyBugRequest(VerifyDecision.RETURN, null, "Still reproducible"));
		assertEquals(BugStatus.RETURNED, bug.getStatus());

		bug.setStatus(BugStatus.NEW);

		assertThrows(
				ConflictException.class,
				() -> bugLifecycleService.verifyBug(
						BUG_ID,
						USER_ID,
						new VerifyBugRequest(VerifyDecision.CONFIRM, null, "Cannot verify")));

		bug.setStatus(BugStatus.NEW);

		bugLifecycleService.closeWithoutFix(
				BUG_ID,
				USER_ID,
				new CloseWithoutFixRequest(BugResolution.NOT_A_BUG, null, "Not a bug"));
		assertEquals(BugStatus.CLOSED, bug.getStatus());

		bug.setStatus(BugStatus.CLOSED);

		bugLifecycleService.reopenBug(
				BUG_ID, USER_ID, new ReopenBugRequest(null, "Issue reappeared"));
		assertEquals(BugStatus.REOPENED, bug.getStatus());

		bug.setStatus(BugStatus.IN_PROGRESS);

		assertThrows(
				ConflictException.class,
				() -> bugLifecycleService.reopenBug(
						BUG_ID, USER_ID, new ReopenBugRequest(null, "Cannot reopen while in progress")));
	}
}
