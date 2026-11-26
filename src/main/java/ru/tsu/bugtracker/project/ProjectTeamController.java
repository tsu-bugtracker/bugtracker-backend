package ru.tsu.bugtracker.project;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.project.dto.AddMemberResultDto;
import ru.tsu.bugtracker.project.dto.AddProjectMemberRequest;
import ru.tsu.bugtracker.project.dto.InvitationDetailsDto;
import ru.tsu.bugtracker.project.dto.ProjectInvitationDto;
import ru.tsu.bugtracker.project.dto.ProjectMemberDto;
import ru.tsu.bugtracker.project.dto.ProjectTeamResponseDto;
import ru.tsu.bugtracker.project.dto.TeamFinalizeResponseDto;
import ru.tsu.bugtracker.project.dto.UpdateMemberRoleRequest;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Project team and invitation endpoints.
 */
@RestController
@Tag(name = "Projects")
public class ProjectTeamController {

	private final ProjectTeamService projectTeamService;

	public ProjectTeamController(ProjectTeamService projectTeamService) {
		this.projectTeamService = projectTeamService;
	}

	@GetMapping("/api/v1/projects/{projectId}/members")
	@Operation(summary = "Get project team")
	public ProjectTeamResponseDto listTeam(@PathVariable UUID projectId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.projectTeamService.listTeam(projectId, currentUser.id());
	}

	@PostMapping("/api/v1/projects/{projectId}/members")
	@Operation(summary = "Add project member")
	public AddMemberResultDto addMember(@PathVariable UUID projectId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody AddProjectMemberRequest request) {
		return this.projectTeamService.addMember(projectId, currentUser.id(), request);
	}

	@PatchMapping("/api/v1/projects/{projectId}/members/{memberId}/role")
	@Operation(summary = "Update project member role")
	public ProjectMemberDto updateMemberRole(@PathVariable UUID projectId, @PathVariable UUID memberId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody UpdateMemberRoleRequest request) {
		return this.projectTeamService.updateMemberRole(projectId, memberId, currentUser.id(), request);
	}

	@DeleteMapping("/api/v1/projects/{projectId}/members/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Remove project member")
	public void removeMember(@PathVariable UUID projectId, @PathVariable UUID memberId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		this.projectTeamService.removeMember(projectId, memberId, currentUser.id());
	}

	@PostMapping("/api/v1/projects/{projectId}/members/finalize")
	@Operation(summary = "Finalize project team")
	public TeamFinalizeResponseDto finalizeTeam(@PathVariable UUID projectId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.projectTeamService.finalizeTeam(projectId, currentUser.id());
	}

	@GetMapping("/api/v1/projects/{projectId}/invitations")
	@Operation(summary = "List project invitations")
	public List<ProjectInvitationDto> listInvitations(@PathVariable UUID projectId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.projectTeamService.listInvitations(projectId, currentUser.id());
	}

	@DeleteMapping("/api/v1/projects/{projectId}/invitations/{invitationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cancel project invitation")
	public void cancelInvitation(@PathVariable UUID projectId, @PathVariable UUID invitationId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		this.projectTeamService.cancelInvitation(projectId, invitationId, currentUser.id());
	}

	@GetMapping("/api/v1/invitations/{token}")
	@SecurityRequirements
	@Operation(summary = "Get invitation by token")
	public InvitationDetailsDto getInvitationByToken(@PathVariable String token) {
		return this.projectTeamService.getInvitationByToken(token);
	}

	@PostMapping("/api/v1/invitations/{token}/accept")
	@Operation(summary = "Accept invitation")
	public ProjectMemberDto acceptInvitation(@PathVariable String token,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.projectTeamService.acceptInvitation(token, currentUser.id());
	}

}
