package ru.tsu.bugtracker.git.dto;

import java.time.Instant;

/**
 * Result of a git synchronization.
 *
 * @param syncedCommitsCount       number of synchronized commits
 * @param syncedMergeRequestsCount number of synchronized merge requests
 * @param syncedAt                 synchronization timestamp
 */
public record GitSyncResult(
		int syncedCommitsCount,
		int syncedMergeRequestsCount,
		Instant syncedAt
) {

}
