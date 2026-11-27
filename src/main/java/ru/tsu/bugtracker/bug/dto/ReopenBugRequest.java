package ru.tsu.bugtracker.bug.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for reopening a closed bug.
 *
 * @param releaseId release in which the bug reappeared
 * @param comment   reason for reopening
 */
public record ReopenBugRequest(
		UUID releaseId,
		@NotBlank String comment
) {
}
