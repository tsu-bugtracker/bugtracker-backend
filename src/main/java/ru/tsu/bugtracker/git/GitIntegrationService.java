package ru.tsu.bugtracker.git;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugSupportService;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.common.exception.UnprocessableEntityException;
import ru.tsu.bugtracker.git.GitProviderClient.GitCommitPayload;
import ru.tsu.bugtracker.git.dto.GitCommitInfo;
import ru.tsu.bugtracker.git.dto.GitIntegrationResponse;
import ru.tsu.bugtracker.git.dto.GitSyncRequest;
import ru.tsu.bugtracker.git.dto.GitSyncResult;
import ru.tsu.bugtracker.git.dto.LinkCommitRequest;
import ru.tsu.bugtracker.git.dto.LinkedCommitDto;
import ru.tsu.bugtracker.git.dto.UpsertGitIntegrationRequest;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.user.User;

/**
 * Application service for project Git integrations, commit synchronization and
 * linking commits or merge requests to bugs.
 */
@Service
@Transactional
public class GitIntegrationService {

	private static final String GIT_INTEGRATION_NOT_FOUND = "Git integration not found";

	private static final String GIT_INTEGRATION_NOT_CONNECTED = "Git integration is not connected";

	private static final String COMMIT_LINK_NOT_FOUND = "Commit link not found";

	private static final String COMMIT_ALREADY_LINKED = "Commit is already linked to this bug";

	private final GitIntegrationRepository gitIntegrationRepository;

	private final GitCommitRepository gitCommitRepository;

	private final BugLinkedCommitRepository bugLinkedCommitRepository;

	private final GitProviderClient gitProviderClient;

	private final ProjectAccessService projectAccessService;

	private final BugSupportService bugSupportService;

	public GitIntegrationService(
			GitIntegrationRepository gitIntegrationRepository,
			GitCommitRepository gitCommitRepository,
			BugLinkedCommitRepository bugLinkedCommitRepository,
			GitProviderClient gitProviderClient,
			ProjectAccessService projectAccessService,
			BugSupportService bugSupportService) {
		this.gitIntegrationRepository = gitIntegrationRepository;
		this.gitCommitRepository = gitCommitRepository;
		this.bugLinkedCommitRepository = bugLinkedCommitRepository;
		this.gitProviderClient = gitProviderClient;
		this.projectAccessService = projectAccessService;
		this.bugSupportService = bugSupportService;
	}

	/**
	 * Returns the connected Git integration of the project, or {@code null} when the
	 * project has no active integration.
	 *
	 * @param projectId project identifier
	 * @return connected integration representation or {@code null}
	 */
	@Transactional(readOnly = true)
	public GitIntegrationResponse findConnectedByProjectId(UUID projectId) {
		return this.gitIntegrationRepository.findByProjectId(projectId)
			.filter(GitIntegration::isConnected)
			.map(GitIntegrationResponse::from)
			.orElse(null);
	}

	/**
	 * Returns the Git integration settings visible to the given user.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @return integration settings
	 */
	@Transactional(readOnly = true)
	public GitIntegrationResponse getGitIntegration(UUID projectId, UUID userId) {
		this.projectAccessService.requireRole(
				projectId, userId, ProjectRole.TEAM_LEAD, ProjectRole.DEVELOPER);
		GitIntegration integration = requireConnectedIntegration(projectId);
		return GitIntegrationResponse.from(integration);
	}

	/**
	 * Creates or updates the Git integration of the project.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @param req       integration payload
	 * @return saved integration settings
	 */
	public GitIntegrationResponse upsertGitIntegration(
			UUID projectId, UUID userId, UpsertGitIntegrationRequest req) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(
				projectId, userId, ProjectRole.TEAM_LEAD, ProjectRole.DEVELOPER);

		GitIntegration integration = this.gitIntegrationRepository.findByProjectId(projectId)
			.orElseGet(GitIntegration::new);
		String effectiveToken = req.accessToken() != null
				? req.accessToken()
				: integration.getAccessToken();
		this.gitProviderClient.verifyRepositoryAccessible(
				req.repositoryUrl(), req.provider(), effectiveToken);

		integration.setProject(project);
		integration.setRepositoryUrl(req.repositoryUrl());
		integration.setProvider(req.provider());
		integration.setAccessToken(effectiveToken);
		integration.setDefaultBranch(req.defaultBranch());
		integration.setConnected(true);
		GitIntegration saved = this.gitIntegrationRepository.save(integration);
		return GitIntegrationResponse.from(saved);
	}

	/**
	 * Connects a Git repository to a project created from a repository URL.
	 *
	 * @param project       created project
	 * @param repositoryUrl repository URL, may be {@code null} or blank
	 */
	public void connectFromProjectCreation(Project project, String repositoryUrl) {
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			return;
		}
		String url = repositoryUrl.trim();
		GitProvider provider = detectProvider(url);
		this.gitProviderClient.verifyRepositoryAccessible(url, provider, null);

		GitIntegration integration = new GitIntegration();
		integration.setProject(project);
		integration.setRepositoryUrl(url);
		integration.setProvider(provider);
		integration.setConnected(true);
		this.gitIntegrationRepository.save(integration);
	}

	/**
	 * Disconnects the Git integration of the project and clears its access token.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 */
	public void disconnectGitIntegration(UUID projectId, UUID userId) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(
				projectId, userId, ProjectRole.TEAM_LEAD, ProjectRole.DEVELOPER);

		GitIntegration integration = requireConnectedIntegration(projectId);
		integration.setConnected(false);
		integration.setAccessToken(null);
		this.gitIntegrationRepository.save(integration);
	}

	/**
	 * Synchronizes commits and merge requests from the connected Git repository.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @param req       optional synchronization parameters
	 * @return synchronization result
	 */
	public GitSyncResult syncGitCommits(UUID projectId, UUID userId, GitSyncRequest req) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.DEVELOPER);

		GitIntegration integration = requireConnectedIntegration(projectId);
		String branch = req != null ? req.branch() : null;
		Instant since = req != null ? req.since() : null;
		List<GitCommitPayload> payloads =
				this.gitProviderClient.fetchCommitsAndMergeRequests(integration, branch, since);

		int syncedCommits = 0;
		int syncedMergeRequests = 0;
		for (GitCommitPayload payload : payloads) {
			if (isDuplicate(projectId, payload)) {
				continue;
			}

			GitCommit commit = new GitCommit();
			commit.setProject(project);
			commit.setType(payload.type());
			commit.setCommitHash(payload.commitHash());
			commit.setMergeRequestUrl(payload.mergeRequestUrl());
			commit.setTitle(payload.title());
			commit.setAuthorName(payload.authorName());
			commit.setCommittedAt(payload.committedAt());
			commit.setBranch(payload.branch());
			this.gitCommitRepository.save(commit);

			if (payload.type() == GitCommitType.MERGE_REQUEST) {
				syncedMergeRequests++;
			}
			else {
				syncedCommits++;
			}
		}

		Instant syncedAt = Instant.now();
		integration.setLastSyncedAt(syncedAt);
		this.gitIntegrationRepository.save(integration);
		return new GitSyncResult(syncedCommits, syncedMergeRequests, syncedAt);
	}

	/**
	 * Returns a filtered page of Git commits belonging to the project.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @param search    optional case-insensitive title or commit hash search
	 * @param type      optional commit type filter
	 * @param page      zero-based page index
	 * @param size      requested page size
	 * @return page of Git commits
	 */
	@Transactional(readOnly = true)
	public PagedResponse<GitCommitInfo> listProjectGitCommits(
			UUID projectId,
			UUID userId,
			String search,
			GitCommitType type,
			int page,
			int size) {
		this.projectAccessService.requireRole(
				projectId, userId, ProjectRole.TEAM_LEAD, ProjectRole.DEVELOPER, ProjectRole.TESTER);

		Specification<GitCommit> specification = buildSpecification(projectId, search, type);
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				Math.clamp(size, 1, 100),
				Sort.by(Sort.Direction.DESC, "committedAt"));
		Page<GitCommitInfo> result = this.gitCommitRepository.findAll(specification, pageable)
			.map(GitCommitInfo::from);
		return PagedResponse.from(result);
	}

	/**
	 * Links a commit or merge request to the bug after verifying it in the connected
	 * Git repository.
	 *
	 * @param bugId  bug identifier
	 * @param userId identifier of the acting user
	 * @param req    commit reference payload
	 * @return created commit link
	 */
	public LinkedCommitDto linkBugCommit(UUID bugId, UUID userId, LinkCommitRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		Project project = this.projectAccessService.requireActiveProject(bug.getProject().getId());
		User actor = this.projectAccessService
			.requireRole(project.getId(), userId, ProjectRole.DEVELOPER)
			.getUser();
		return verifyAndLinkCommit(
				bug, actor, req.commitHash(), req.mergeRequestUrl(), true);
	}

	/**
	 * Verifies a commit or merge request in the connected Git repository and links it
	 * to the bug.
	 *
	 * @param bug              bug to link
	 * @param actor            user performing the link
	 * @param commitHash       optional commit hash
	 * @param mergeRequestUrl  optional merge request URL
	 * @param failOnDuplicate  whether an already linked commit must cause a conflict
	 * @return created commit link
	 */
	public LinkedCommitDto verifyAndLinkCommit(
			Bug bug,
			User actor,
			String commitHash,
			String mergeRequestUrl,
			boolean failOnDuplicate) {
		GitIntegration integration = this.gitIntegrationRepository
			.findByProjectId(bug.getProject().getId())
			.filter(GitIntegration::isConnected)
			.orElseThrow(() -> new UnprocessableEntityException(GIT_INTEGRATION_NOT_CONNECTED));

		GitCommitPayload payload = this.gitProviderClient.verifyCommitOrMergeRequest(
				integration, commitHash, mergeRequestUrl);

		if (failOnDuplicate && isAlreadyLinked(bug.getId(), payload)) {
			throw new ConflictException(COMMIT_ALREADY_LINKED);
		}

		BugLinkedCommit link = new BugLinkedCommit();
		link.setBug(bug);
		link.setType(payload.type());
		link.setCommitHash(payload.commitHash());
		link.setMergeRequestUrl(payload.mergeRequestUrl());
		link.setAuthorName(payload.authorName());
		link.setCommittedAt(payload.committedAt());
		link.setLinkedBy(actor);
		BugLinkedCommit saved = this.bugLinkedCommitRepository.save(link);

		String refValue = payload.commitHash() != null
				? payload.commitHash()
				: payload.mergeRequestUrl();
		this.bugSupportService.recordHistory(bug, actor, "commits", null, refValue);
		return LinkedCommitDto.from(saved);
	}

	/**
	 * Unlinks a commit or merge request from the bug.
	 *
	 * @param bugId        bug identifier
	 * @param commitLinkId commit link identifier
	 * @param userId       identifier of the acting user
	 */
	public void unlinkBugCommit(UUID bugId, UUID commitLinkId, UUID userId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.DEVELOPER)
			.getUser();

		BugLinkedCommit link = this.bugLinkedCommitRepository
			.findByIdAndBugId(commitLinkId, bugId)
			.orElseThrow(() -> new NotFoundException(COMMIT_LINK_NOT_FOUND));
		this.bugLinkedCommitRepository.delete(link);

		String refValue = link.getCommitHash() != null
				? link.getCommitHash()
				: link.getMergeRequestUrl();
		this.bugSupportService.recordHistory(bug, actor, "commits", refValue, null);
	}

	private GitIntegration requireConnectedIntegration(UUID projectId) {
		return this.gitIntegrationRepository.findByProjectId(projectId)
			.filter(GitIntegration::isConnected)
			.orElseThrow(() -> new NotFoundException(GIT_INTEGRATION_NOT_FOUND));
	}

	private GitProvider detectProvider(String repositoryUrl) {
		String normalized = repositoryUrl.toLowerCase(Locale.ROOT);
		if (normalized.contains("github.com")) {
			return GitProvider.GITHUB;
		}
		if (normalized.contains("gitlab")) {
			return GitProvider.GITLAB;
		}
		return GitProvider.CUSTOM;
	}

	private boolean isDuplicate(UUID projectId, GitCommitPayload payload) {
		if (payload.commitHash() != null && this.gitCommitRepository
			.findByProjectIdAndTypeAndCommitHash(
					projectId, payload.type(), payload.commitHash())
			.isPresent()) {
			return true;
		}
		return payload.mergeRequestUrl() != null && this.gitCommitRepository
			.findByProjectIdAndTypeAndMergeRequestUrl(
					projectId, payload.type(), payload.mergeRequestUrl())
			.isPresent();
	}

	private boolean isAlreadyLinked(UUID bugId, GitCommitPayload payload) {
		if (payload.commitHash() != null && this.bugLinkedCommitRepository
			.existsByBugIdAndTypeAndCommitHash(
					bugId, payload.type(), payload.commitHash())) {
			return true;
		}
		return payload.mergeRequestUrl() != null && this.bugLinkedCommitRepository
			.existsByBugIdAndTypeAndMergeRequestUrl(
					bugId, payload.type(), payload.mergeRequestUrl());
	}

	private Specification<GitCommit> buildSpecification(
			UUID projectId, String search, GitCommitType type) {
		return (root, _, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("project").get("id"), projectId));
			if (type != null) {
				predicates.add(cb.equal(root.get("type"), type));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("title")), pattern),
						cb.like(cb.lower(root.get("commitHash")), pattern)));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

}
