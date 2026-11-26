package ru.tsu.bugtracker.project.dto;

/**
 * Result of adding a member to a project.
 *
 * @param outcome    {@code "MEMBER_ADDED"} when the user was added directly,
 *                   {@code "INVITATION_SENT"} when an invitation was created
 * @param member     created membership, or {@code null} when an invitation was sent
 * @param invitation created invitation, or {@code null} when the user was added directly
 */
public record AddMemberResultDto(
		String outcome,
		ProjectMemberDto member,
		ProjectInvitationDto invitation
) {
}
