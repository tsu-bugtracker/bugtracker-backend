package com.bugtracker.controller;

import com.bugtracker.dto.ProjectMemberAddDto;
import com.bugtracker.dto.ProjectMemberResponseDto;
import com.bugtracker.dto.ProjectMemberUpdateDto;
import com.bugtracker.service.ProjectMemberService;
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
@RequestMapping("/api/v1/projects/{projectId}/members")
@RequiredArgsConstructor
@Tag(name = "Project Members", description = "Project team management endpoints")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a member to a project")
    @ApiResponse(responseCode = "201", description = "Member added")
    @ApiResponse(responseCode = "404", description = "Project or user not found")
    @ApiResponse(responseCode = "409", description = "User is already a project member")
    public ProjectMemberResponseDto addMember(@PathVariable UUID projectId,
                                              @Valid @RequestBody ProjectMemberAddDto dto) {
        return projectMemberService.addMember(projectId, dto);
    }

    @GetMapping
    @Operation(summary = "Get all members of a project")
    @ApiResponse(responseCode = "200", description = "List of project members")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public List<ProjectMemberResponseDto> getMembers(@PathVariable UUID projectId) {
        return projectMemberService.getMembers(projectId);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update a project member role")
    @ApiResponse(responseCode = "200", description = "Member role updated")
    @ApiResponse(responseCode = "404", description = "Project member not found")
    public ProjectMemberResponseDto updateMemberRole(@PathVariable UUID projectId,
                                                     @PathVariable UUID userId,
                                                     @Valid @RequestBody ProjectMemberUpdateDto dto) {
        return projectMemberService.updateMemberRole(projectId, userId, dto);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from a project")
    @ApiResponse(responseCode = "204", description = "Member removed")
    @ApiResponse(responseCode = "404", description = "Project member not found")
    @ApiResponse(responseCode = "422", description = "Cannot remove the only team lead")
    public void removeMember(@PathVariable UUID projectId, @PathVariable UUID userId) {
        projectMemberService.removeMember(projectId, userId);
    }
}
