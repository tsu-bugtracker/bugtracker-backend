package ru.tsu.bugtracker.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import ru.tsu.bugtracker.bug.BugSupportService;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.common.exception.UnprocessableEntityException;
import ru.tsu.bugtracker.git.GitProviderClient.GitCommitPayload;
import ru.tsu.bugtracker.git.dto.GitIntegrationResponse;
import ru.tsu.bugtracker.git.dto.GitSyncRequest;
import ru.tsu.bugtracker.git.dto.GitSyncResult;
import ru.tsu.bugtracker.git.dto.LinkCommitRequest;
import ru.tsu.bugtracker.git.dto.LinkedCommitDto;
import ru.tsu.bugtracker.git.dto.UpsertGitIntegrationRequest;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.user.User;

@ExtendWith(MockitoExtension.class)
class GitIntegrationServiceTest {

	private static final UUID PROJECT_ID = UUID.randomUUID();

	private static final UUID USER_ID = UUID.randomUUID();

	private static final UUID BUG_ID = UUID.randomUUID();

	private static final UUID INTEGRATION_ID = UUID.randomUUID();

	private static final UUID COMMIT_LINK_ID = UUID.randomUUID();

	private static final Instant COMMITTED_AT = Instant.parse("2026-11-20T10:15:30Z");

	private static final Instant SINCE = Instant.parse("2026-11-01T00:00:00Z");

	private static final String REPOSITORY_URL = "https://github.com/tsu/bugtracker";

	private static final String COMMIT_HASH = "abc123def456";

	private static final String MERGE_REQUEST_URL = "https://github.com/tsu/bugtracker/pull/42";

	@Mock
	private GitIntegrationRepository gitIntegrationRepository;

	@Mock
	private GitCommitRepository gitCommitRepository;

	@Mock
	private BugLinkedCommitRepository bugLinkedCommitRepository;

	@Mock
	private GitProviderClient gitProviderClient;

	@Mock
	private ProjectAccessService projectAccessService;

	@Mock
	private BugSupportService bugSupportService;

	@InjectMocks
	private GitIntegrationService gitIntegrationService;

	private Project project;

	private User actor;

	private Bug bug;

	private ProjectMember membership;

	@BeforeEach
	void setUp() {
		project = new Project();
		project.setId(PROJECT_ID);

		actor = new User();
		actor.setId(USER_ID);
		actor.setEmail("dev@tsu.ru");
		actor.setFullName("Developer");

		bug = new Bug();
		bug.setId(BUG_ID);
		bug.setProject(project);

		membership = new ProjectMember();
		membership.setProject(project);
		membership.setUser(actor);
		membership.setRole(ProjectRole.DEVELOPER);
	}

	@Test
	void upsertGitIntegration_createsIntegrationAndReturnsConfiguration() {
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(eq(PROJECT_ID), eq(USER_ID), any(ProjectRole[].class)))
			.thenReturn(membership);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
		when(gitIntegrationRepository.save(any(GitIntegration.class))).thenAnswer(invocation -> {
			GitIntegration saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(INTEGRATION_ID);
			}
			return saved;
		});

		GitIntegrationResponse result = gitIntegrationService.upsertGitIntegration(
				PROJECT_ID,
				USER_ID,
				new UpsertGitIntegrationRequest(REPOSITORY_URL, GitProvider.GITHUB, "secret-token", "main"));

		verify(gitProviderClient)
			.verifyRepositoryAccessible(REPOSITORY_URL, GitProvider.GITHUB, "secret-token");

		ArgumentCaptor<GitIntegration> integrationCaptor = ArgumentCaptor.forClass(GitIntegration.class);
		verify(gitIntegrationRepository).save(integrationCaptor.capture());
		GitIntegration saved = integrationCaptor.getValue();

		assertSame(project, saved.getProject());
		assertEquals(REPOSITORY_URL, saved.getRepositoryUrl());
		assertEquals(GitProvider.GITHUB, saved.getProvider());
		assertEquals("secret-token", saved.getAccessToken());
		assertEquals("main", saved.getDefaultBranch());
		assertTrue(saved.isConnected());

		assertEquals(INTEGRATION_ID, result.id());
		assertEquals(PROJECT_ID, result.projectId());
		assertEquals(REPOSITORY_URL, result.repositoryUrl());
		assertEquals(GitProvider.GITHUB, result.provider());
		assertEquals("main", result.defaultBranch());
		assertTrue(result.hasAccessToken());
		assertTrue(result.connected());
	}

	@Test
	void upsertGitIntegration_updatesExistingIntegrationAndKeepsStoredToken() {
		GitIntegration existing = connectedIntegration("stored-token");
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(eq(PROJECT_ID), eq(USER_ID), any(ProjectRole[].class)))
			.thenReturn(membership);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(existing));
		when(gitIntegrationRepository.save(existing)).thenReturn(existing);

		GitIntegrationResponse result = gitIntegrationService.upsertGitIntegration(
				PROJECT_ID,
				USER_ID,
				new UpsertGitIntegrationRequest(
						"https://github.com/tsu/renamed", GitProvider.GITHUB, null, "develop"));

		verify(gitProviderClient).verifyRepositoryAccessible(
				"https://github.com/tsu/renamed", GitProvider.GITHUB, "stored-token");
		verify(gitIntegrationRepository).save(existing);

		assertEquals(INTEGRATION_ID, existing.getId());
		assertEquals("https://github.com/tsu/renamed", existing.getRepositoryUrl());
		assertEquals("stored-token", existing.getAccessToken());
		assertEquals("develop", existing.getDefaultBranch());
		assertTrue(existing.isConnected());

		assertEquals(INTEGRATION_ID, result.id());
		assertTrue(result.hasAccessToken());
		assertTrue(result.connected());
	}

	@Test
	void syncGitCommits_savesOnlyNewCommitsAndMergeRequests() {
		GitIntegration integration = connectedIntegration("secret-token");
		GitCommitPayload newCommit = payload(GitCommitType.COMMIT, "new-commit", null);
		GitCommitPayload duplicateCommit = payload(GitCommitType.COMMIT, "duplicate-commit", null);
		GitCommitPayload newMergeRequest =
				payload(GitCommitType.MERGE_REQUEST, null, "https://github.com/tsu/bugtracker/pull/7");
		GitCommitPayload duplicateMergeRequest = payload(
				GitCommitType.MERGE_REQUEST, null, "https://github.com/tsu/bugtracker/pull/8");

		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(eq(PROJECT_ID), eq(USER_ID), any(ProjectRole[].class)))
			.thenReturn(membership);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.fetchCommitsAndMergeRequests(integration, "develop", SINCE))
			.thenReturn(List.of(newCommit, duplicateCommit, newMergeRequest, duplicateMergeRequest));
		lenient().when(gitCommitRepository.findByProjectIdAndTypeAndCommitHash(
				PROJECT_ID, GitCommitType.COMMIT, "duplicate-commit"))
			.thenReturn(Optional.of(new GitCommit()));
		lenient().when(gitCommitRepository.findByProjectIdAndTypeAndMergeRequestUrl(
				PROJECT_ID, GitCommitType.MERGE_REQUEST, "https://github.com/tsu/bugtracker/pull/8"))
			.thenReturn(Optional.of(new GitCommit()));

		GitSyncResult result = gitIntegrationService.syncGitCommits(
				PROJECT_ID, USER_ID, new GitSyncRequest("develop", SINCE));

		verify(gitProviderClient).fetchCommitsAndMergeRequests(integration, "develop", SINCE);

		ArgumentCaptor<GitCommit> commitCaptor = ArgumentCaptor.forClass(GitCommit.class);
		verify(gitCommitRepository, times(2)).save(commitCaptor.capture());
		List<GitCommit> savedCommits = commitCaptor.getAllValues();

		assertEquals(2, savedCommits.size());
		assertEquals("new-commit", savedCommits.get(0).getCommitHash());
		assertEquals(GitCommitType.COMMIT, savedCommits.get(0).getType());
		assertEquals("https://github.com/tsu/bugtracker/pull/7", savedCommits.get(1).getMergeRequestUrl());
		assertEquals(GitCommitType.MERGE_REQUEST, savedCommits.get(1).getType());
		assertSame(project, savedCommits.get(0).getProject());
		assertSame(project, savedCommits.get(1).getProject());

		assertEquals(1, result.syncedCommitsCount());
		assertEquals(1, result.syncedMergeRequestsCount());
		assertNotNull(result.syncedAt());
		assertEquals(result.syncedAt(), integration.getLastSyncedAt());
		verify(gitIntegrationRepository).save(integration);
	}

	@Test
	void verifyAndLinkCommit_throwsUnprocessableEntity_whenIntegrationIsMissing() {
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());

		UnprocessableEntityException exception = assertThrows(
				UnprocessableEntityException.class,
				() -> gitIntegrationService.verifyAndLinkCommit(bug, actor, COMMIT_HASH, null, true));

		assertEquals("Git integration is not connected", exception.getMessage());
		verifyNoInteractions(gitProviderClient, bugLinkedCommitRepository, bugSupportService);
	}

	@Test
	void verifyAndLinkCommit_throwsUnprocessableEntity_whenIntegrationIsDisconnected() {
		GitIntegration disconnected = connectedIntegration("secret-token");
		disconnected.setConnected(false);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(disconnected));

		UnprocessableEntityException exception = assertThrows(
				UnprocessableEntityException.class,
				() -> gitIntegrationService.verifyAndLinkCommit(bug, actor, COMMIT_HASH, null, true));

		assertEquals("Git integration is not connected", exception.getMessage());
		verifyNoInteractions(gitProviderClient, bugLinkedCommitRepository, bugSupportService);
	}

	@Test
	void verifyAndLinkCommit_throwsUnprocessableEntity_whenCommitCannotBeVerified() {
		GitIntegration integration = connectedIntegration("secret-token");
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.verifyCommitOrMergeRequest(integration, "missing-commit", null))
			.thenThrow(new UnprocessableEntityException("Commit or merge request was not found in Git provider"));

		assertThrows(
				UnprocessableEntityException.class,
				() -> gitIntegrationService.verifyAndLinkCommit(bug, actor, "missing-commit", null, true));

		verify(bugLinkedCommitRepository, never()).save(any(BugLinkedCommit.class));
		verify(bugSupportService, never()).recordHistory(any(), any(), anyString(), any(), any());
	}

	@Test
	void verifyAndLinkCommit_savesLinkAndRecordsHistory() {
		GitIntegration integration = connectedIntegration("secret-token");
		GitCommitPayload payload = payload(GitCommitType.COMMIT, COMMIT_HASH, null);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.verifyCommitOrMergeRequest(integration, COMMIT_HASH, null))
			.thenReturn(payload);
		when(bugLinkedCommitRepository
			.existsByBugIdAndTypeAndCommitHash(BUG_ID, GitCommitType.COMMIT, COMMIT_HASH))
			.thenReturn(false);
		when(bugLinkedCommitRepository.save(any(BugLinkedCommit.class))).thenAnswer(invocation -> {
			BugLinkedCommit link = invocation.getArgument(0);
			link.setId(COMMIT_LINK_ID);
			return link;
		});

		LinkedCommitDto result = gitIntegrationService
			.verifyAndLinkCommit(bug, actor, COMMIT_HASH, null, true);

		ArgumentCaptor<BugLinkedCommit> linkCaptor = ArgumentCaptor.forClass(BugLinkedCommit.class);
		verify(bugLinkedCommitRepository).save(linkCaptor.capture());
		BugLinkedCommit saved = linkCaptor.getValue();

		assertSame(bug, saved.getBug());
		assertEquals(GitCommitType.COMMIT, saved.getType());
		assertEquals(COMMIT_HASH, saved.getCommitHash());
		assertEquals("Developer", saved.getAuthorName());
		assertEquals(COMMITTED_AT, saved.getCommittedAt());
		assertSame(actor, saved.getLinkedBy());

		assertEquals(COMMIT_LINK_ID, result.id());
		assertEquals(BUG_ID, result.bugId());
		assertEquals(GitCommitType.COMMIT, result.type());
		assertEquals(COMMIT_HASH, result.commitHash());
		assertEquals(USER_ID, result.linkedBy().id());

		verify(bugSupportService)
			.recordHistory(eq(bug), eq(actor), eq("commits"), isNull(), eq(COMMIT_HASH));
	}

	@Test
	void verifyAndLinkCommit_recordsMergeRequestUrlForMergeRequest() {
		GitIntegration integration = connectedIntegration("secret-token");
		GitCommitPayload payload = payload(GitCommitType.MERGE_REQUEST, null, MERGE_REQUEST_URL);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.verifyCommitOrMergeRequest(integration, null, MERGE_REQUEST_URL))
			.thenReturn(payload);
		when(bugLinkedCommitRepository
			.existsByBugIdAndTypeAndMergeRequestUrl(BUG_ID, GitCommitType.MERGE_REQUEST, MERGE_REQUEST_URL))
			.thenReturn(false);
		when(bugLinkedCommitRepository.save(any(BugLinkedCommit.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		LinkedCommitDto result = gitIntegrationService
			.verifyAndLinkCommit(bug, actor, null, MERGE_REQUEST_URL, true);

		assertEquals(GitCommitType.MERGE_REQUEST, result.type());
		assertEquals(MERGE_REQUEST_URL, result.mergeRequestUrl());
		verify(bugSupportService)
			.recordHistory(eq(bug), eq(actor), eq("commits"), isNull(), eq(MERGE_REQUEST_URL));
	}

	@Test
	void verifyAndLinkCommit_throwsConflict_whenCommitIsAlreadyLinkedAndFailOnDuplicate() {
		GitIntegration integration = connectedIntegration("secret-token");
		GitCommitPayload payload = payload(GitCommitType.COMMIT, COMMIT_HASH, null);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.verifyCommitOrMergeRequest(integration, COMMIT_HASH, null))
			.thenReturn(payload);
		when(bugLinkedCommitRepository
			.existsByBugIdAndTypeAndCommitHash(BUG_ID, GitCommitType.COMMIT, COMMIT_HASH))
			.thenReturn(true);

		ConflictException exception = assertThrows(
				ConflictException.class,
				() -> gitIntegrationService.verifyAndLinkCommit(bug, actor, COMMIT_HASH, null, true));

		assertEquals("Commit is already linked to this bug", exception.getMessage());
		verify(bugLinkedCommitRepository, never()).save(any(BugLinkedCommit.class));
		verify(bugSupportService, never()).recordHistory(any(), any(), anyString(), any(), any());
	}

	@Test
	void verifyAndLinkCommit_allowsDuplicate_whenFailOnDuplicateIsFalse() {
		GitIntegration integration = connectedIntegration("secret-token");
		GitCommitPayload payload = payload(GitCommitType.COMMIT, COMMIT_HASH, null);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.verifyCommitOrMergeRequest(integration, COMMIT_HASH, null))
			.thenReturn(payload);
		when(bugLinkedCommitRepository.save(any(BugLinkedCommit.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		gitIntegrationService.verifyAndLinkCommit(bug, actor, COMMIT_HASH, null, false);

		verify(bugLinkedCommitRepository, never())
			.existsByBugIdAndTypeAndCommitHash(any(), any(), anyString());
		verify(bugLinkedCommitRepository).save(any(BugLinkedCommit.class));
		verify(bugSupportService)
			.recordHistory(eq(bug), eq(actor), eq("commits"), isNull(), eq(COMMIT_HASH));
	}

	@Test
	void linkBugCommit_resolvesActorAndLinksCommit() {
		GitIntegration integration = connectedIntegration("secret-token");
		GitCommitPayload payload = payload(GitCommitType.COMMIT, COMMIT_HASH, null);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER))
			.thenReturn(membership);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(integration));
		when(gitProviderClient.verifyCommitOrMergeRequest(integration, COMMIT_HASH, null))
			.thenReturn(payload);
		when(bugLinkedCommitRepository.save(any(BugLinkedCommit.class))).thenAnswer(invocation -> {
			BugLinkedCommit link = invocation.getArgument(0);
			link.setId(COMMIT_LINK_ID);
			return link;
		});

		LinkedCommitDto result = gitIntegrationService.linkBugCommit(
				BUG_ID, USER_ID, new LinkCommitRequest(COMMIT_HASH, null));

		assertEquals(COMMIT_LINK_ID, result.id());
		assertEquals(BUG_ID, result.bugId());
		assertEquals(COMMIT_HASH, result.commitHash());
		verify(bugSupportService)
			.recordHistory(eq(bug), eq(actor), eq("commits"), isNull(), eq(COMMIT_HASH));
	}

	@Test
	void linkBugCommit_throwsUnprocessableEntity_whenIntegrationIsDisconnected() {
		GitIntegration disconnected = connectedIntegration("secret-token");
		disconnected.setConnected(false);
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER))
			.thenReturn(membership);
		when(gitIntegrationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.of(disconnected));

		assertThrows(
				UnprocessableEntityException.class,
				() -> gitIntegrationService.linkBugCommit(
						BUG_ID, USER_ID, new LinkCommitRequest(COMMIT_HASH, null)));

		verifyNoInteractions(gitProviderClient);
		verify(bugLinkedCommitRepository, never()).save(any(BugLinkedCommit.class));
	}

	@Test
	void unlinkBugCommit_deletesLinkAndRecordsHistory() {
		BugLinkedCommit link = new BugLinkedCommit();
		link.setId(COMMIT_LINK_ID);
		link.setBug(bug);
		link.setType(GitCommitType.COMMIT);
		link.setCommitHash(COMMIT_HASH);
		link.setLinkedBy(actor);

		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER))
			.thenReturn(membership);
		when(bugLinkedCommitRepository.findByIdAndBugId(COMMIT_LINK_ID, BUG_ID))
			.thenReturn(Optional.of(link));

		gitIntegrationService.unlinkBugCommit(BUG_ID, COMMIT_LINK_ID, USER_ID);

		verify(bugLinkedCommitRepository).delete(link);
		verify(bugSupportService)
			.recordHistory(eq(bug), eq(actor), eq("commits"), eq(COMMIT_HASH), isNull());
	}

	@Test
	void unlinkBugCommit_recordsMergeRequestUrlWhenCommitHashIsAbsent() {
		BugLinkedCommit link = new BugLinkedCommit();
		link.setId(COMMIT_LINK_ID);
		link.setBug(bug);
		link.setType(GitCommitType.MERGE_REQUEST);
		link.setMergeRequestUrl(MERGE_REQUEST_URL);

		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER))
			.thenReturn(membership);
		when(bugLinkedCommitRepository.findByIdAndBugId(COMMIT_LINK_ID, BUG_ID))
			.thenReturn(Optional.of(link));

		gitIntegrationService.unlinkBugCommit(BUG_ID, COMMIT_LINK_ID, USER_ID);

		verify(bugLinkedCommitRepository).delete(link);
		verify(bugSupportService)
			.recordHistory(eq(bug), eq(actor), eq("commits"), eq(MERGE_REQUEST_URL), isNull());
	}

	@Test
	void unlinkBugCommit_throwsNotFound_whenLinkDoesNotExist() {
		when(bugSupportService.requireBug(BUG_ID)).thenReturn(bug);
		when(projectAccessService.requireActiveProject(PROJECT_ID)).thenReturn(project);
		when(projectAccessService.requireRole(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER))
			.thenReturn(membership);
		when(bugLinkedCommitRepository.findByIdAndBugId(COMMIT_LINK_ID, BUG_ID))
			.thenReturn(Optional.empty());

		assertThrows(
				NotFoundException.class,
				() -> gitIntegrationService.unlinkBugCommit(BUG_ID, COMMIT_LINK_ID, USER_ID));

		verify(bugLinkedCommitRepository, never()).delete(any(BugLinkedCommit.class));
		verify(bugSupportService, never()).recordHistory(any(), any(), anyString(), any(), any());
	}

	private GitIntegration connectedIntegration(String accessToken) {
		GitIntegration integration = new GitIntegration();
		integration.setId(INTEGRATION_ID);
		integration.setProject(project);
		integration.setRepositoryUrl(REPOSITORY_URL);
		integration.setProvider(GitProvider.GITHUB);
		integration.setAccessToken(accessToken);
		integration.setDefaultBranch("main");
		integration.setConnected(true);
		return integration;
	}

	private GitCommitPayload payload(GitCommitType type, String commitHash, String mergeRequestUrl) {
		return new GitCommitPayload(
				type,
				commitHash,
				mergeRequestUrl,
				"Fix null pointer",
				"Developer",
				COMMITTED_AT,
				"develop");
	}

}
