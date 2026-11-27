package ru.tsu.bugtracker.release;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.release.dto.CreateReleaseRequest;
import ru.tsu.bugtracker.release.dto.PublishReleaseRequest;
import ru.tsu.bugtracker.release.dto.ReleaseDetailsDto;
import ru.tsu.bugtracker.release.dto.ReleaseSummaryDto;
import ru.tsu.bugtracker.release.dto.SetReleaseBugsRequest;
import ru.tsu.bugtracker.release.dto.UpdateReleaseRequest;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Release management endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Releases")
public class ReleaseController {

	private final ReleaseService releaseService;

	public ReleaseController(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@GetMapping("/projects/{projectId}/releases")
	@Operation(summary = "List project releases")
	public PagedResponse<ReleaseSummaryDto> listProjectReleases(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@RequestParam(required = false) ReleaseStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.releaseService.listProjectReleases(projectId, currentUser.id(), status, page, size);
	}

	@PostMapping("/projects/{projectId}/releases")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create release")
	public ReleaseDetailsDto createRelease(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateReleaseRequest request) {
		return this.releaseService.createRelease(projectId, currentUser.id(), request);
	}

	@GetMapping("/releases/{releaseId}")
	@Operation(summary = "Get release details")
	public ReleaseDetailsDto getRelease(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID releaseId) {
		return this.releaseService.getRelease(releaseId, currentUser.id());
	}

	@PatchMapping("/releases/{releaseId}")
	@Operation(summary = "Update release")
	public ReleaseDetailsDto updateRelease(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID releaseId,
			@Valid @RequestBody UpdateReleaseRequest request) {
		return this.releaseService.updateRelease(releaseId, currentUser.id(), request);
	}

	@DeleteMapping("/releases/{releaseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete release")
	public void deleteRelease(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID releaseId) {
		this.releaseService.deleteRelease(releaseId, currentUser.id());
	}

	@PutMapping("/releases/{releaseId}/bugs")
	@Operation(summary = "Set release bugs")
	public ReleaseDetailsDto setReleaseBugs(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID releaseId,
			@Valid @RequestBody SetReleaseBugsRequest request) {
		return this.releaseService.setReleaseBugs(releaseId, currentUser.id(), request);
	}

	@PostMapping("/releases/{releaseId}/publish")
	@Operation(summary = "Publish release")
	public ReleaseDetailsDto publishRelease(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID releaseId,
			@Valid @RequestBody(required = false) PublishReleaseRequest request) {
		return this.releaseService.publishRelease(releaseId, currentUser.id(), request);
	}

}
