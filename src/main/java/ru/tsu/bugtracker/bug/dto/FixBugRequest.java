package ru.tsu.bugtracker.bug.dto;

import java.util.List;

/**
 * Payload for submitting a fix for a bug.
 *
 * @param commits commit and merge request references
 * @param comment optional comment describing the fix
 */
public record FixBugRequest(
		List<CommitReferenceInputDto> commits,
		String comment
) {
}
