package ru.tsu.bugtracker.planning;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.bug.BugStatus;
import ru.tsu.bugtracker.bug.dto.BugDetailsDto;
import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.planning.dto.AssignBugRequest;
import ru.tsu.bugtracker.planning.dto.FixPlanRequest;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Planning workflow endpoints: fix plans, bug assignment and personal task lists.
 */
@RestController
@Tag(name = "Planning")
public class PlanningController {

	private final PlanningService planningService;

	public PlanningController(PlanningService planningService) {
		this.planningService = planningService;
	}

	@PutMapping("/api/v1/bugs/{bugId}/plan")
	@Operation(summary = "Plan a bug fix")
	public BugDetailsDto planBugFix(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody FixPlanRequest request) {
		return this.planningService.planBugFix(bugId, currentUser.id(), request);
	}

	@PostMapping("/api/v1/bugs/{bugId}/assign")
	@Operation(summary = "Assign a bug")
	public BugDetailsDto assignBug(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody AssignBugRequest request) {
		return this.planningService.assignBug(bugId, currentUser.id(), request);
	}

	@PostMapping("/api/v1/bugs/{bugId}/take")
	@Operation(summary = "Take a bug in work")
	public BugDetailsDto takeBugInWork(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.planningService.takeBugInWork(bugId, currentUser.id());
	}

	@GetMapping("/api/v1/projects/{projectId}/backlog")
	@Operation(summary = "Get project backlog")
	public PagedResponse<BugSummaryDto> getProjectBacklog(@PathVariable UUID projectId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@RequestParam(required = false) List<BugPriority> priority,
			@RequestParam(required = false) UUID componentId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.planningService.getProjectBacklog(
				projectId, currentUser.id(), priority, componentId, PageRequest.of(page, size));
	}

	@GetMapping("/api/v1/tasks/my")
	@Operation(summary = "Get my tasks")
	public PagedResponse<BugSummaryDto> getMyTasks(@AuthenticationPrincipal AuthenticatedUser currentUser,
			@RequestParam(required = false) UUID projectId,
			@RequestParam(required = false) List<BugStatus> status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.planningService.getMyTasks(
				currentUser.id(), projectId, status, PageRequest.of(page, size));
	}
}
