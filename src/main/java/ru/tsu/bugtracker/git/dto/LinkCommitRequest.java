package ru.tsu.bugtracker.git.dto;

/**
 * Payload for linking a commit or merge request to a bug.
 *
 * @param commitHash      commit hash
 * @param mergeRequestUrl merge request URL
 */
public record LinkCommitRequest(
		String commitHash,
		String mergeRequestUrl
) {

}
