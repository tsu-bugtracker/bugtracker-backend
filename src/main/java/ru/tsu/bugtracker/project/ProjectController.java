package ru.tsu.bugtracker.project;

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

import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.project.dto.CreateProjectRequest;
import ru.tsu.bugtracker.project.dto.ProjectDetailsDto;
import ru.tsu.bugtracker.project.dto.ProjectSummaryDto;
import ru.tsu.bugtracker.project.dto.UpdateProjectRequest;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Project management endpoints.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	@Operation(summary = "List projects", description = "Returns projects available to the current user with optional status filter and name search")
	public PagedResponse<ProjectSummaryDto> listProjects(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@RequestParam(required = false) ProjectStatus status,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.projectService.listProjects(currentUser.id(), status, search, PageRequest.of(page, size));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create project", description = "Creates a project and makes the current user its team lead")
	public ProjectDetailsDto createProject(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody CreateProjectRequest request) {
		return this.projectService.createProject(currentUser.id(), request);
	}

	@GetMapping("/{projectId}")
	@Operation(summary = "Get project details")
	public ProjectDetailsDto getProject(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId) {
		return this.projectService.getProject(projectId, currentUser.id());
	}

	@PatchMapping("/{projectId}")
	@Operation(summary = "Update project")
	public ProjectDetailsDto updateProject(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody UpdateProjectRequest request) {
		return this.projectService.updateProject(projectId, currentUser.id(), request);
	}

	@PostMapping("/{projectId}/archive")
	@Operation(summary = "Archive project")
	public ProjectDetailsDto archiveProject(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId) {
		return this.projectService.archiveProject(projectId, currentUser.id());
	}

}
