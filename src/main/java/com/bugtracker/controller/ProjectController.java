package com.bugtracker.controller;

import com.bugtracker.dto.ProjectCreateDto;
import com.bugtracker.dto.ProjectResponseDto;
import com.bugtracker.dto.ProjectUpdateDto;
import com.bugtracker.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new project")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "404", description = "Creator user not found")
    @ApiResponse(responseCode = "409", description = "Project name already exists")
    public ProjectResponseDto createProject(@Valid @RequestBody ProjectCreateDto dto) {
        return projectService.createProject(dto);
    }

    @GetMapping
    @Operation(summary = "Get all projects")
    @ApiResponse(responseCode = "200", description = "List of projects")
    public List<ProjectResponseDto> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project by id")
    @ApiResponse(responseCode = "200", description = "Project found")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public ProjectResponseDto getProjectById(@PathVariable UUID id) {
        return projectService.getProjectById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing project")
    @ApiResponse(responseCode = "200", description = "Project updated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @ApiResponse(responseCode = "409", description = "Project name already exists")
    public ProjectResponseDto updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectUpdateDto dto) {
        return projectService.updateProject(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a project")
    @ApiResponse(responseCode = "204", description = "Project deactivated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public void deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
    }
}
