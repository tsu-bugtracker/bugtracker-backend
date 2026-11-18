package com.bugtracker.controller;

import com.bugtracker.domain.enums.IssuePriority;
import com.bugtracker.domain.enums.IssueStatus;
import com.bugtracker.dto.IssueCloseDto;
import com.bugtracker.dto.IssueCreateDto;
import com.bugtracker.dto.IssueLinkCreateDto;
import com.bugtracker.dto.IssueLinkDto;
import com.bugtracker.dto.IssueResponseDto;
import com.bugtracker.dto.IssueStatusTransitionDto;
import com.bugtracker.dto.IssueUpdateDto;
import com.bugtracker.service.IssueLinkService;
import com.bugtracker.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Issue tracking and lifecycle management endpoints")
public class IssueController {

    private final IssueService issueService;
    private final IssueLinkService issueLinkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new issue")
    @ApiResponse(responseCode = "201", description = "Issue created")
    @ApiResponse(responseCode = "404", description = "Project, reporter, component, release or tag not found")
    @ApiResponse(responseCode = "422", description = "Bug tracking is disabled or reporter has no TESTER role")
    public IssueResponseDto createIssue(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Valid @RequestBody IssueCreateDto dto) {
        return issueService.createIssue(projectId, dto);
    }

    @GetMapping
    @Operation(summary = "Get issues of a project with optional filters")
    @ApiResponse(responseCode = "200", description = "List of issues")
    public List<IssueResponseDto> getIssues(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Filter by issue status")
            @RequestParam(required = false) IssueStatus status,
            @Parameter(description = "Filter by issue priority")
            @RequestParam(required = false) IssuePriority priority,
            @Parameter(description = "Filter by assignee user identifier")
            @RequestParam(required = false) UUID assigneeId,
            @Parameter(description = "Filter by reporter user identifier")
            @RequestParam(required = false) UUID reporterId,
            @Parameter(description = "Filter by component identifier")
            @RequestParam(required = false) UUID componentId,
            @Parameter(description = "Filter by release identifier")
            @RequestParam(required = false) UUID releaseId) {
        return issueService.getIssues(projectId, status, priority, assigneeId, reporterId, componentId, releaseId);
    }

    @GetMapping("/{issueId}")
    @Operation(summary = "Get an issue by id")
    @ApiResponse(responseCode = "200", description = "Issue found")
    @ApiResponse(responseCode = "404", description = "Issue not found in the project")
    public IssueResponseDto getIssueById(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId) {
        return issueService.getIssueById(projectId, issueId);
    }

    @PutMapping("/{issueId}")
    @Operation(summary = "Update issue metadata")
    @ApiResponse(responseCode = "200", description = "Issue updated")
    @ApiResponse(responseCode = "404", description = "Issue, component, release or tag not found in the project")
    @ApiResponse(responseCode = "422", description = "Assignee has an invalid project role")
    public IssueResponseDto updateIssue(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId,
            @Valid @RequestBody IssueUpdateDto dto) {
        return issueService.updateIssue(projectId, issueId, dto);
    }

    @PatchMapping("/{issueId}/status")
    @Operation(summary = "Change the status of an issue")
    @ApiResponse(responseCode = "200", description = "Issue status changed")
    @ApiResponse(responseCode = "404", description = "Issue or project member not found")
    @ApiResponse(responseCode = "422", description = "Invalid status transition or insufficient project role")
    public IssueResponseDto changeStatus(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId,
            @Parameter(description = "Identifier of the user performing the transition", required = true)
            @RequestParam UUID userId,
            @Valid @RequestBody IssueStatusTransitionDto dto) {
        return issueService.changeStatus(projectId, issueId, userId, dto);
    }

    @PatchMapping("/{issueId}/close")
    @Operation(summary = "Close an issue with a resolution")
    @ApiResponse(responseCode = "200", description = "Issue closed")
    @ApiResponse(responseCode = "404", description = "Issue, project member or fix release not found")
    @ApiResponse(responseCode = "422", description = "Invalid resolution or insufficient project role")
    public IssueResponseDto closeIssue(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId,
            @Parameter(description = "Identifier of the user closing the issue", required = true)
            @RequestParam UUID userId,
            @Valid @RequestBody IssueCloseDto dto) {
        return issueService.closeIssue(projectId, issueId, userId, dto);
    }

    @PostMapping("/{issueId}/reopen")
    @Operation(summary = "Reopen a closed issue")
    @ApiResponse(responseCode = "200", description = "Issue reopened")
    @ApiResponse(responseCode = "404", description = "Issue or project member not found")
    @ApiResponse(responseCode = "422", description = "Issue is not closed or user has insufficient project role")
    public IssueResponseDto reopenIssue(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId,
            @Parameter(description = "Identifier of the user reopening the issue", required = true)
            @RequestParam UUID userId) {
        return issueService.reopenIssue(projectId, issueId, userId);
    }

    @PostMapping("/{issueId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a semantic link between two issues")
    @ApiResponse(responseCode = "201", description = "Link created")
    @ApiResponse(responseCode = "404", description = "Source or target issue not found in the project")
    @ApiResponse(responseCode = "409", description = "Link already exists")
    @ApiResponse(responseCode = "422", description = "An issue cannot be linked to itself")
    public IssueLinkDto addLink(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Source issue identifier", required = true)
            @PathVariable UUID issueId,
            @Valid @RequestBody IssueLinkCreateDto dto) {
        return issueLinkService.addLink(projectId, issueId, dto);
    }

    @GetMapping("/{issueId}/links")
    @Operation(summary = "Get all semantic links of an issue")
    @ApiResponse(responseCode = "200", description = "List of issue links")
    @ApiResponse(responseCode = "404", description = "Issue not found in the project")
    public List<IssueLinkDto> getLinks(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId) {
        return issueLinkService.getLinks(projectId, issueId);
    }

    @DeleteMapping("/{issueId}/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a semantic link from an issue")
    @ApiResponse(responseCode = "204", description = "Link removed")
    @ApiResponse(responseCode = "404", description = "Issue or link not found")
    public void removeLink(
            @Parameter(description = "Project identifier", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "Issue identifier", required = true)
            @PathVariable UUID issueId,
            @Parameter(description = "Link identifier", required = true)
            @PathVariable UUID linkId) {
        issueLinkService.removeLink(projectId, issueId, linkId);
    }
}
