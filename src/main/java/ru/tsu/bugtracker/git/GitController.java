package ru.tsu.bugtracker.git;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.git.dto.GitCommitInfo;
import ru.tsu.bugtracker.git.dto.GitIntegrationResponse;
import ru.tsu.bugtracker.git.dto.GitSyncRequest;
import ru.tsu.bugtracker.git.dto.GitSyncResult;
import ru.tsu.bugtracker.git.dto.LinkCommitRequest;
import ru.tsu.bugtracker.git.dto.LinkedCommitDto;
import ru.tsu.bugtracker.git.dto.UpsertGitIntegrationRequest;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Git integration endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Git Integration")
public class GitController {

	private final GitIntegrationService gitIntegrationService;

	public GitController(GitIntegrationService gitIntegrationService) {
		this.gitIntegrationService = gitIntegrationService;
	}

	@GetMapping("/projects/{projectId}/git-integration")
	@Operation(summary = "Get project Git integration")
	public GitIntegrationResponse getGitIntegration(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId) {
		return this.gitIntegrationService.getGitIntegration(projectId, currentUser.id());
	}

	@PutMapping("/projects/{projectId}/git-integration")
	@Operation(summary = "Save project Git integration")
	public GitIntegrationResponse upsertGitIntegration(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody UpsertGitIntegrationRequest request) {
		return this.gitIntegrationService.upsertGitIntegration(projectId, currentUser.id(), request);
	}

	@DeleteMapping("/projects/{projectId}/git-integration")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Disconnect project Git integration")
	public void disconnectGitIntegration(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId) {
		this.gitIntegrationService.disconnectGitIntegration(projectId, currentUser.id());
	}

	@PostMapping("/projects/{projectId}/git-integration/sync")
	@Operation(summary = "Sync project Git commits")
	public GitSyncResult syncGitCommits(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@RequestBody(required = false) GitSyncRequest request) {
		return this.gitIntegrationService.syncGitCommits(projectId, currentUser.id(), request);
	}

	@GetMapping("/projects/{projectId}/git-commits")
	@Operation(summary = "List project Git commits")
	public PagedResponse<GitCommitInfo> listProjectGitCommits(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) GitCommitType type,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.gitIntegrationService.listProjectGitCommits(projectId, currentUser.id(), search, type, page, size);
	}

	@PostMapping("/bugs/{bugId}/commits")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Link commit to bug")
	public LinkedCommitDto linkBugCommit(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody LinkCommitRequest request) {
		return this.gitIntegrationService.linkBugCommit(bugId, currentUser.id(), request);
	}

	@DeleteMapping("/bugs/{bugId}/commits/{commitLinkId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Unlink commit from bug")
	public void unlinkBugCommit(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@PathVariable UUID commitLinkId) {
		this.gitIntegrationService.unlinkBugCommit(bugId, commitLinkId, currentUser.id());
	}

}
