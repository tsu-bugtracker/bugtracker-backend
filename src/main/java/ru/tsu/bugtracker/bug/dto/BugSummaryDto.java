package ru.tsu.bugtracker.bug.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.bug.BugResolution;
import ru.tsu.bugtracker.bug.BugStatus;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.project.dto.LabelDto;
import ru.tsu.bugtracker.project.dto.SoftwareComponentDto;
import ru.tsu.bugtracker.release.dto.ReleaseShortDto;

/**
 * Compact bug representation used in lists and search results.
 *
 * @param id                  bug identifier
 * @param projectId           owning project identifier
 * @param key                 human-readable bug key
 * @param title               short summary
 * @param status              lifecycle status
 * @param priority            current priority
 * @param recommendedPriority priority recommended by the reporter
 * @param resolution          resolution of a closed bug
 * @param author              user who reported the bug
 * @param assignee            user currently assigned to the bug
 * @param component           affected software component
 * @param labels              labels attached to the bug
 * @param targetRelease       release the bug is planned for
 * @param deadline            planned fix deadline
 * @param overdue             whether the deadline has passed
 * @param createdAt           creation timestamp
 * @param updatedAt           last update timestamp
 */
public record BugSummaryDto(
		UUID id,
		UUID projectId,
		String key,
		String title,
		BugStatus status,
		BugPriority priority,
		BugPriority recommendedPriority,
		BugResolution resolution,
		UserShortDto author,
		UserShortDto assignee,
		SoftwareComponentDto component,
		List<LabelDto> labels,
		ReleaseShortDto targetRelease,
		Instant deadline,
		boolean overdue,
		Instant createdAt,
		Instant updatedAt
) {
}
