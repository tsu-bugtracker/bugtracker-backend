package ru.tsu.bugtracker.git.dto;

import java.time.Instant;

/**
 * Payload for triggering a git synchronization.
 *
 * @param branch optional branch to sync
 * @param since  optional lower bound for synchronized items
 */
public record GitSyncRequest(
		String branch,
		Instant since
) {

}
