package ru.tsu.bugtracker.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.bug.BugSupportService;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.planning.FixPlanRepository;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.release.dto.CreateReleaseRequest;
import ru.tsu.bugtracker.release.dto.PublishReleaseRequest;
import ru.tsu.bugtracker.release.dto.ReleaseDetailsDto;
import ru.tsu.bugtracker.release.dto.SetReleaseBugsRequest;
import ru.tsu.bugtracker.release.dto.UpdateReleaseRequest;
import ru.tsu.bugtracker.user.User;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

	private static final UUID RELEASE_ID = UUID.randomUUID();
	private static final UUID PROJECT_ID = UUID.randomUUID();
	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID FIRST_BUG_ID = UUID.randomUUID();
	private static final UUID SECOND_BUG_ID = UUID.randomUUID();
	private static final LocalDate PLANNED_DATE = LocalDate.of(2026, 12, 1);

	@Mock
	private ReleaseRepository releaseRepository;

	@Mock
	private BugRepository bugRepository;

	@Mock
	private FixPlanRepository fixPlanRepository;

	@Mock
	private ProjectAccessService projectAccessService;

	@Mock
	private BugSupportService bugSupportService;

	@InjectMocks
	private ReleaseService releaseService;

	private Project project;
	private User actor;
	private Release release;

	@BeforeEach
	void setUp() {
		project = new Project();
		project.setId(PROJECT_ID);

		actor = new User();
		actor.setId(USER_ID);

		release = new Release();
		release.setId(RELEASE_ID);
		release.setProject(project);
		release.setVersion("1.0.0");
		release.setStatus(ReleaseStatus.PLANNED);
		release.setCreatedBy(actor);

		ProjectMember membership = new ProjectMember();
		membership.setProject(project);
		membership.setUser(actor);
		membership.setRole(ProjectRole.TEAM_LEAD);

		lenient().when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(eq(PROJECT_ID), eq(USER_ID), any(ProjectRole[].class)))
			.thenReturn(membership);
		lenient().when(releaseRepository.save(any(Release.class))).thenAnswer(invocation -> {
			Release saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(RELEASE_ID);
			}
			return saved;
		});
		lenient().when(bugRepository.findAllByReleaseId(any())).thenReturn(List.of());
	}

	@Test
	void createRelease_createsPlannedReleaseAndAttachesBugs() {
		Bug firstBug = bug(FIRST_BUG_ID);
		Bug secondBug = bug(SECOND_BUG_ID);

		when(releaseRepository.existsByProjectIdAndVersionIgnoreCase(PROJECT_ID, "1.2.0"))
			.thenReturn(false);
		when(bugRepository.findAllByProjectIdAndIdIn(eq(PROJECT_ID), any()))
			.thenReturn(List.of(firstBug, secondBug));

		ReleaseDetailsDto result = releaseService.createRelease(
				PROJECT_ID,
				USER_ID,
				new CreateReleaseRequest(
						" 1.2.0 ",
						"Version 1.2",
						"Third public iteration",
						PLANNED_DATE,
						List.of(FIRST_BUG_ID, SECOND_BUG_ID)));

		ArgumentCaptor<Release> releaseCaptor = ArgumentCaptor.forClass(Release.class);
		verify(releaseRepository).save(releaseCaptor.capture());
		Release saved = releaseCaptor.getValue();

		assertEquals(ReleaseStatus.PLANNED, saved.getStatus());
		assertEquals("1.2.0", saved.getVersion());
		assertEquals("Version 1.2", saved.getName());
		assertEquals(project, saved.getProject());
		assertEquals(actor, saved.getCreatedBy());

		assertEquals(RELEASE_ID, result.id());
		assertEquals(PROJECT_ID, result.projectId());
		assertEquals("1.2.0", result.version());
		assertEquals(ReleaseStatus.PLANNED, result.status());

		assertSame(saved, firstBug.getTargetRelease());
		assertSame(saved, secondBug.getTargetRelease());
		verify(bugRepository).saveAll(List.of(firstBug, secondBug));
	}

	@Test
	void createRelease_throwsConflict_whenVersionAlreadyExists() {
		when(releaseRepository.existsByProjectIdAndVersionIgnoreCase(PROJECT_ID, "1.0.0"))
			.thenReturn(true);

		assertThrows(
				ConflictException.class,
				() -> releaseService.createRelease(
						PROJECT_ID,
						USER_ID,
						new CreateReleaseRequest("1.0.0", null, null, null, null)));

		verify(releaseRepository, never()).save(any(Release.class));
	}

	@Test
	void updateRelease_throwsConflict_whenReleased() {
		release.setStatus(ReleaseStatus.RELEASED);

		assertThrows(
				ConflictException.class,
				() -> releaseService.updateRelease(
						RELEASE_ID,
						USER_ID,
						new UpdateReleaseRequest("2.0.0", "Next version", null, PLANNED_DATE)));

		verify(releaseRepository, never()).save(any(Release.class));
	}

	@Test
	void deleteRelease_throwsConflict_whenReleased() {
		release.setStatus(ReleaseStatus.RELEASED);

		assertThrows(
				ConflictException.class,
				() -> releaseService.deleteRelease(RELEASE_ID, USER_ID));

		verify(releaseRepository, never()).delete(any(Release.class));
		verify(bugRepository, never()).clearTargetRelease(any());
	}

	@Test
	void setReleaseBugs_throwsConflict_whenReleased() {
		release.setStatus(ReleaseStatus.RELEASED);

		assertThrows(
				ConflictException.class,
				() -> releaseService.setReleaseBugs(
						RELEASE_ID, USER_ID, new SetReleaseBugsRequest(List.of(FIRST_BUG_ID))));

		verify(bugRepository, never()).findAllByReleaseId(any());
		verify(bugRepository, never()).saveAll(any());
	}

	@Test
	void publishRelease_throwsConflict_whenAlreadyReleased() {
		release.setStatus(ReleaseStatus.RELEASED);

		assertThrows(
				ConflictException.class,
				() -> releaseService.publishRelease(
						RELEASE_ID, USER_ID, new PublishReleaseRequest("Notes")));

		verify(releaseRepository, never()).save(any(Release.class));
	}

	@Test
	void setReleaseBugs_detachesOldBugsAndAttachesNewOnes() {
		Bug oldBug = bug(FIRST_BUG_ID);
		oldBug.setTargetRelease(release);
		Bug newBug = bug(SECOND_BUG_ID);

		when(bugRepository.findAllByReleaseId(RELEASE_ID)).thenReturn(List.of(oldBug));
		when(bugRepository.findAllByProjectIdAndIdIn(eq(PROJECT_ID), any()))
			.thenReturn(List.of(newBug));

		releaseService.setReleaseBugs(
				RELEASE_ID, USER_ID, new SetReleaseBugsRequest(List.of(SECOND_BUG_ID)));

		assertNull(oldBug.getTargetRelease());
		assertSame(release, newBug.getTargetRelease());
		verify(bugRepository).saveAll(List.of(oldBug));
		verify(bugRepository).saveAll(List.of(newBug));
	}

	@Test
	void setReleaseBugs_throwsBadRequest_whenBugBelongsToAnotherProject() {
		when(bugRepository.findAllByProjectIdAndIdIn(eq(PROJECT_ID), any()))
			.thenReturn(List.of());

		assertThrows(
				BadRequestException.class,
				() -> releaseService.setReleaseBugs(
						RELEASE_ID, USER_ID, new SetReleaseBugsRequest(List.of(FIRST_BUG_ID))));

		verify(bugRepository, never()).saveAll(any());
	}

	@Test
	void publishRelease_movesPlannedReleaseToReleased() {
		ReleaseDetailsDto result = releaseService.publishRelease(
				RELEASE_ID, USER_ID, new PublishReleaseRequest("  First public build  "));

		assertEquals(ReleaseStatus.RELEASED, result.status());
		assertNotNull(result.releasedAt());
		assertEquals("First public build", result.releaseNotes());

		assertEquals(ReleaseStatus.RELEASED, release.getStatus());
		assertNotNull(release.getReleasedAt());
		assertEquals("First public build", release.getReleaseNotes());
		verify(releaseRepository).save(release);
	}

	@Test
	void deleteRelease_clearsBugAndPlanReferencesAndDeletesRelease() {
		releaseService.deleteRelease(RELEASE_ID, USER_ID);

		verify(bugRepository).clearTargetRelease(RELEASE_ID);
		verify(bugRepository).clearFixedInRelease(RELEASE_ID);
		verify(fixPlanRepository).clearTargetRelease(RELEASE_ID);
		verify(releaseRepository).delete(release);
	}

	private Bug bug(UUID id) {
		Bug bug = new Bug();
		bug.setId(id);
		bug.setProject(project);
		return bug;
	}
}
