package ru.tsu.bugtracker.bug.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ru.tsu.bugtracker.bug.BugLinkType;

/**
 * Payload for linking the current bug to another bug.
 *
 * @param targetBugId identifier of the bug to link to
 * @param linkType    type of the link
 */
public record CreateBugLinkRequest(
		@NotNull UUID targetBugId,
		@NotNull BugLinkType linkType
) {
}
