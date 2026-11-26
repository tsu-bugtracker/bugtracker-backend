package ru.tsu.bugtracker.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ru.tsu.bugtracker.project.ProjectRole;

/**
 * Request payload for adding a member to a project.
 *
 * @param email email address of the user to add
 * @param role  role to grant
 */
public record AddProjectMemberRequest(
		@NotBlank @Email String email,
		@NotNull ProjectRole role
) {
}
