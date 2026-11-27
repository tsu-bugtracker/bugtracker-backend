package ru.tsu.bugtracker.release.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import ru.tsu.bugtracker.release.ReleaseStatus;

/**
 * Compact release representation used in release lists.
 *
 * @param id           release identifier
 * @param projectId    owning project identifier
 * @param version      release version
 * @param name         release name
 * @param status       lifecycle status
 * @param plannedDate  planned release date
 * @param releasedAt   actual release timestamp
 * @param bugsCount    number of bugs associated with the release
 * @param createdAt    creation timestamp
 */
public record ReleaseSummaryDto(
		UUID id,
		UUID projectId,
		String version,
		String name,
		ReleaseStatus status,
		LocalDate plannedDate,
		Instant releasedAt,
		long bugsCount,
		Instant createdAt
) {
}
