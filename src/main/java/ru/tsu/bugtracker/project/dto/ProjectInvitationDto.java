package ru.tsu.bugtracker.project.dto;

import java.time.Instant;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.project.InvitationStatus;
import ru.tsu.bugtracker.project.ProjectInvitation;
import ru.tsu.bugtracker.project.ProjectRole;

/**
 * Project invitation representation.
 *
 * @param id        invitation identifier
 * @param email     invited email address
 * @param role      role offered by the invitation
 * @param status    invitation status
 * @param invitedBy user who sent the invitation
 * @param expiresAt expiration timestamp
 * @param createdAt creation timestamp
 */
public record ProjectInvitationDto(
		UUID id,
		String email,
		ProjectRole role,
		InvitationStatus status,
		UserShortDto invitedBy,
		Instant expiresAt,
		Instant createdAt
) {

	public static ProjectInvitationDto from(ProjectInvitation invitation) {
		return new ProjectInvitationDto(
				invitation.getId(),
				invitation.getEmail(),
				invitation.getRole(),
				invitation.getStatus(),
				UserShortDto.from(invitation.getInvitedBy()),
				invitation.getExpiresAt(),
				invitation.getCreatedAt()
		);
	}

}
