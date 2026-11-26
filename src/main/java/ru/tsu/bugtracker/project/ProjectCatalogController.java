package ru.tsu.bugtracker.project;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.project.dto.CreateComponentRequest;
import ru.tsu.bugtracker.project.dto.CreateLabelRequest;
import ru.tsu.bugtracker.project.dto.LabelDto;
import ru.tsu.bugtracker.project.dto.MergeLabelsRequest;
import ru.tsu.bugtracker.project.dto.SoftwareComponentDto;
import ru.tsu.bugtracker.project.dto.UpdateComponentRequest;
import ru.tsu.bugtracker.project.dto.UpdateLabelRequest;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Project-scoped endpoints for software components and labels.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@Tag(name = "Projects")
public class ProjectCatalogController {

	private final ProjectCatalogService projectCatalogService;

	public ProjectCatalogController(ProjectCatalogService projectCatalogService) {
		this.projectCatalogService = projectCatalogService;
	}

	@GetMapping("/components")
	@Operation(summary = "List software components")
	public List<SoftwareComponentDto> listComponents(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId) {
		return this.projectCatalogService.listComponents(projectId, currentUser.id());
	}

	@PostMapping("/components")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create software component")
	public SoftwareComponentDto createComponent(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateComponentRequest request) {
		return this.projectCatalogService.createComponent(projectId, currentUser.id(), request);
	}

	@PatchMapping("/components/{componentId}")
	@Operation(summary = "Update software component")
	public SoftwareComponentDto updateComponent(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@PathVariable UUID componentId,
			@Valid @RequestBody UpdateComponentRequest request) {
		return this.projectCatalogService.updateComponent(projectId, componentId, currentUser.id(), request);
	}

	@DeleteMapping("/components/{componentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete software component")
	public void deleteComponent(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@PathVariable UUID componentId) {
		this.projectCatalogService.deleteComponent(projectId, componentId, currentUser.id());
	}

	@GetMapping("/labels")
	@Operation(summary = "List labels")
	public List<LabelDto> listLabels(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId) {
		return this.projectCatalogService.listLabels(projectId, currentUser.id());
	}

	@PostMapping("/labels")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create label")
	public LabelDto createLabel(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateLabelRequest request) {
		return this.projectCatalogService.createLabel(projectId, currentUser.id(), request);
	}

	@PatchMapping("/labels/{labelId}")
	@Operation(summary = "Update label")
	public LabelDto updateLabel(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@PathVariable UUID labelId,
			@Valid @RequestBody UpdateLabelRequest request) {
		return this.projectCatalogService.updateLabel(projectId, labelId, currentUser.id(), request);
	}

	@DeleteMapping("/labels/{labelId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete label")
	public void deleteLabel(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@PathVariable UUID labelId) {
		this.projectCatalogService.deleteLabel(projectId, labelId, currentUser.id());
	}

	@PostMapping("/labels/merge")
	@Operation(summary = "Merge labels")
	public LabelDto mergeLabels(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID projectId,
			@Valid @RequestBody MergeLabelsRequest request) {
		return this.projectCatalogService.mergeLabels(projectId, currentUser.id(), request);
	}

}
