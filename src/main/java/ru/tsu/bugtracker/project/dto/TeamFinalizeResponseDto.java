package ru.tsu.bugtracker.project.dto;

/**
 * Result of finalizing the project team.
 *
 * @param bugTrackingEnabled whether bug tracking was enabled
 * @param hasActiveTester    whether the project has an active tester
 * @param warning            optional warning message
 */
public record TeamFinalizeResponseDto(
		boolean bugTrackingEnabled,
		boolean hasActiveTester,
		String warning
) {
}
