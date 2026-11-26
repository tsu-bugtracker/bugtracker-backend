package ru.tsu.bugtracker.bug.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload with additional information supplied by a bug reporter.
 *
 * @param stepsToReproduce updated reproduction steps
 * @param environment      updated environment description
 * @param comment          clarification comment
 */
public record ClarifyBugRequest(
		String stepsToReproduce,
		String environment,
		@NotBlank String comment
) {
}
