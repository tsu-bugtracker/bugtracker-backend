package ru.tsu.bugtracker.bug.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ru.tsu.bugtracker.bug.BugResolution;

/**
 * Payload for closing a bug without a fix.
 *
 * @param resolution       reason for closing the bug
 * @param duplicateOfBugId original bug when the resolution is {@code DUPLICATE}
 * @param comment          optional closing comment
 */
public record CloseWithoutFixRequest(
		@NotNull BugResolution resolution,
		UUID duplicateOfBugId,
		String comment
) {
}
