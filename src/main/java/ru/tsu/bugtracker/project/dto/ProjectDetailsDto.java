package ru.tsu.bugtracker.project.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.project.ProjectStatus;

/**
 * Detailed project representation.
 *
 * @param id                 project identifier
 * @param name               project name
 * @param description        free-form description
 * @param techStack          technologies used by the project
 * @param status             lifecycle status
 * @param bugTrackingEnabled whether bug tracking is enabled
 * @param hasActiveTester    whether the project has an active tester
 * @param owner              project owner
 * @param myRole             role of the current user in the project
 * @param gitIntegration     Git integration details; {@code null} at this stage
 * @param startDate          planned start date
 * @param endDate            planned end date
 * @param membersCount       number of project members
 * @param createdAt          creation timestamp
 * @param updatedAt          last update timestamp
 */
public record ProjectDetailsDto(
		UUID id,
		String name,
		String description,
		String techStack,
		ProjectStatus status,
		boolean bugTrackingEnabled,
		boolean hasActiveTester,
		UserShortDto owner,
		ProjectRole myRole,
		Object gitIntegration,
		LocalDate startDate,
		LocalDate endDate,
		long membersCount,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
