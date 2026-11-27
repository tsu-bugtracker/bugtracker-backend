package ru.tsu.bugtracker.release.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for replacing the bugs associated with a release.
 *
 * @param bugIds new bug identifiers
 */
public record SetReleaseBugsRequest(
		@NotNull List<UUID> bugIds
) {
}
