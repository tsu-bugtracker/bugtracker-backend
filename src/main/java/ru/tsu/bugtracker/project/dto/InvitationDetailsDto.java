package ru.tsu.bugtracker.project.dto;

import java.time.Instant;
import java.util.UUID;

import ru.tsu.bugtracker.project.InvitationStatus;
import ru.tsu.bugtracker.project.ProjectRole;

/**
 * Public details of a project invitation resolved by token.
 *
 * @param projectId   project identifier
 * @param projectName project name
 * @param email       invited email address
 * @param role        role offered by the invitation
 * @param status      invitation status
 * @param expiresAt   expiration timestamp
 */
public record InvitationDetailsDto(
		UUID projectId,
		String projectName,
		String email,
		ProjectRole role,
		InvitationStatus status,
		Instant expiresAt
) {
}
