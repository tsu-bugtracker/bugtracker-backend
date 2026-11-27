package ru.tsu.bugtracker.planning.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for assigning a bug to a user.
 *
 * @param assigneeId identifier of the user to assign
 */
public record AssignBugRequest(
		@NotNull UUID assigneeId
) {
}
