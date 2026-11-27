package ru.tsu.bugtracker.bug.dto;

/**
 * Reference to a commit or merge request containing a fix.
 *
 * @param commitHash      commit hash
 * @param mergeRequestUrl merge request URL
 */
public record CommitReferenceInputDto(
		String commitHash,
		String mergeRequestUrl
) {
}
