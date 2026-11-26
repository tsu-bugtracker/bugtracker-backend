package ru.tsu.bugtracker.project.dto;

import jakarta.validation.constraints.NotNull;

import ru.tsu.bugtracker.project.ProjectRole;

/**
 * Request payload for changing a project member's role.
 *
 * @param role new role
 */
public record UpdateMemberRoleRequest(
		@NotNull ProjectRole role
) {
}
