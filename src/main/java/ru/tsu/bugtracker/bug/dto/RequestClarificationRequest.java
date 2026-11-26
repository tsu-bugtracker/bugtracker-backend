package ru.tsu.bugtracker.bug.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for requesting additional information about a bug.
 *
 * @param question question addressed to the reporter
 */
public record RequestClarificationRequest(
		@NotBlank String question
) {
}
