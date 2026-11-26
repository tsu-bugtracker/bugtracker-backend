package ru.tsu.bugtracker.project.dto;

import java.util.List;

/**
 * Project team representation: members plus pending invitations.
 *
 * @param members            project members
 * @param invitations        project invitations
 * @param hasTeamLead        whether the project has a team lead
 * @param hasActiveTester    whether the project has an active tester
 * @param bugTrackingEnabled whether bug tracking is enabled
 */
public record ProjectTeamResponseDto(
		List<ProjectMemberDto> members,
		List<ProjectInvitationDto> invitations,
		boolean hasTeamLead,
		boolean hasActiveTester,
		boolean bugTrackingEnabled
) {
}
