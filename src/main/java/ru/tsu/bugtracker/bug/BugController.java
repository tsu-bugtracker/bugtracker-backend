package ru.tsu.bugtracker.bug;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.bug.dto.BugDetailsDto;
import ru.tsu.bugtracker.bug.dto.BugHistoryEntryDto;
import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.bug.dto.ClarifyBugRequest;
import ru.tsu.bugtracker.bug.dto.CloseWithoutFixRequest;
import ru.tsu.bugtracker.bug.dto.CreateBugRequest;
import ru.tsu.bugtracker.bug.dto.FixBugRequest;
import ru.tsu.bugtracker.bug.dto.ReopenBugRequest;
import ru.tsu.bugtracker.bug.dto.RequestClarificationRequest;
import ru.tsu.bugtracker.bug.dto.UpdateBugRequest;
import ru.tsu.bugtracker.bug.dto.VerifyBugRequest;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Bug tracking endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bugs")
public class BugController {

	private final BugService bugService;

	private final BugLifecycleService bugLifecycleService;

	public BugController(BugService bugService, BugLifecycleService bugLifecycleService) {
		this.bugService = bugService;
		this.bugLifecycleService = bugLifecycleService;
	}

	@GetMapping("/projects/{projectId}/bugs")
	@Operation(summary = "List project bugs", description = "Returns a filtered page of bugs for the given project")
	public PagedResponse<BugSummaryDto> listProjectBugs(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@RequestParam(required = false) BugStatus status,
			@RequestParam(required = false) BugPriority priority,
			@RequestParam(required = false) UUID assigneeId,
			@RequestParam(required = false) UUID componentId,
			@RequestParam(required = false) UUID labelId,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean overdue,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.bugService.listProjectBugs(projectId, currentUser.id(), status, priority, assigneeId,
				componentId, labelId, search, overdue, PageRequest.of(page, size));
	}

	@PostMapping("/projects/{projectId}/bugs")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create bug", description = "Creates a bug in the given project")
	public BugDetailsDto createBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateBugRequest request) {
		return this.bugService.createBug(projectId, currentUser.id(), request);
	}

	@GetMapping("/bugs/{bugId}")
	@Operation(summary = "Get bug details")
	public BugDetailsDto getBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId) {
		return this.bugService.getBug(bugId, currentUser.id());
	}

	@PatchMapping("/bugs/{bugId}")
	@Operation(summary = "Update bug")
	public BugDetailsDto updateBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody UpdateBugRequest request) {
		return this.bugService.updateBug(bugId, currentUser.id(), request);
	}

	@PostMapping("/bugs/{bugId}/request-clarification")
	@Operation(summary = "Request clarification", description = "Moves the bug from IN_PROGRESS to NEEDS_INFO")
	public BugDetailsDto requestClarification(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody RequestClarificationRequest request) {
		return this.bugLifecycleService.requestClarification(bugId, currentUser.id(), request);
	}

	@PostMapping("/bugs/{bugId}/clarify")
	@Operation(summary = "Clarify bug", description = "Moves the bug from NEEDS_INFO back to IN_PROGRESS")
	public BugDetailsDto clarifyBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody ClarifyBugRequest request) {
		return this.bugLifecycleService.clarifyBug(bugId, currentUser.id(), request);
	}

	@PostMapping("/bugs/{bugId}/fix")
	@Operation(summary = "Submit fix", description = "Moves the bug from IN_PROGRESS or RETURNED to IN_REVIEW")
	public BugDetailsDto fixBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody(required = false) FixBugRequest request) {
		return this.bugLifecycleService.fixBug(bugId, currentUser.id(), request);
	}

	@PostMapping("/bugs/{bugId}/verify")
	@Operation(summary = "Verify fix", description = "Applies a verification decision to a bug in IN_REVIEW")
	public BugDetailsDto verifyBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody VerifyBugRequest request) {
		return this.bugLifecycleService.verifyBug(bugId, currentUser.id(), request);
	}

	@PostMapping("/bugs/{bugId}/close-without-fix")
	@Operation(summary = "Close bug without fix")
	public BugDetailsDto closeWithoutFix(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody CloseWithoutFixRequest request) {
		return this.bugLifecycleService.closeWithoutFix(bugId, currentUser.id(), request);
	}

	@PostMapping("/bugs/{bugId}/reopen")
	@Operation(summary = "Reopen bug", description = "Moves a CLOSED bug to REOPENED")
	public BugDetailsDto reopenBug(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@Valid @RequestBody ReopenBugRequest request) {
		return this.bugLifecycleService.reopenBug(bugId, currentUser.id(), request);
	}

	@GetMapping("/bugs/{bugId}/history")
	@Operation(summary = "Get bug history", description = "Returns a page of change history entries for the bug")
	public PagedResponse<BugHistoryEntryDto> getBugHistory(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID bugId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.bugService.getBugHistory(bugId, currentUser.id(), PageRequest.of(page, size));
	}

}
