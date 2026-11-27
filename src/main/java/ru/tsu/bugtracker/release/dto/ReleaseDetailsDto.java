package ru.tsu.bugtracker.release.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.release.ReleaseStatus;

/**
 * Detailed release representation.
 *
 * @param id           release identifier
 * @param projectId    owning project identifier
 * @param version      release version
 * @param name         release name
 * @param description  free-form description
 * @param releaseNotes notes published with the release
 * @param status       lifecycle status
 * @param plannedDate  planned release date
 * @param releasedAt   actual release timestamp
 * @param bugsCount    number of bugs associated with the release
 * @param bugs         bugs associated with the release
 * @param createdAt    creation timestamp
 * @param updatedAt    last update timestamp
 */
public record ReleaseDetailsDto(
		UUID id,
		UUID projectId,
		String version,
		String name,
		String description,
		String releaseNotes,
		ReleaseStatus status,
		LocalDate plannedDate,
		Instant releasedAt,
		long bugsCount,
		List<BugSummaryDto> bugs,
		Instant createdAt,
		Instant updatedAt
) {
}
