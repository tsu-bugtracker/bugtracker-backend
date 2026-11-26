package ru.tsu.bugtracker.project.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.project.ProjectStatus;

/**
 * Compact project representation used in project lists.
 *
 * @param id                 project identifier
 * @param name               project name
 * @param description        free-form description
 * @param techStack          technologies used by the project
 * @param status             lifecycle status
 * @param bugTrackingEnabled whether bug tracking is enabled
 * @param myRole             role of the current user in the project
 * @param openBugsCount      number of open bugs
 * @param membersCount       number of project members
 * @param startDate          planned start date
 * @param endDate            planned end date
 * @param createdAt          creation timestamp
 */
public record ProjectSummaryDto(
		UUID id,
		String name,
		String description,
		String techStack,
		ProjectStatus status,
		boolean bugTrackingEnabled,
		ProjectRole myRole,
		long openBugsCount,
		long membersCount,
		LocalDate startDate,
		LocalDate endDate,
		OffsetDateTime createdAt
) {
}
