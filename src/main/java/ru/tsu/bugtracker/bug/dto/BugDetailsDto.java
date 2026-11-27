package ru.tsu.bugtracker.bug.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.bug.BugResolution;
import ru.tsu.bugtracker.bug.BugStatus;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.planning.dto.FixPlanDto;
import ru.tsu.bugtracker.project.dto.LabelDto;
import ru.tsu.bugtracker.project.dto.SoftwareComponentDto;

/**
 * Full bug representation returned for a single bug.
 *
 * @param id                  bug identifier
 * @param projectId           owning project identifier
 * @param key                 human-readable bug key
 * @param title               short summary
 * @param description         detailed description
 * @param stepsToReproduce    steps needed to reproduce the bug
 * @param environment         environment where the bug was observed
 * @param expectedResult      expected behaviour
 * @param actualResult        actual behaviour
 * @param status              lifecycle status
 * @param priority            current priority
 * @param recommendedPriority priority recommended by the reporter
 * @param resolution          resolution of a closed bug
 * @param author              user who reported the bug
 * @param assignee            user currently assigned to the bug
 * @param component           affected software component
 * @param labels              labels attached to the bug
 * @param fixPlan             planning details for the bug
 * @param targetRelease       release the bug is planned for; reserved
 * @param fixedInRelease      release that contains the fix; reserved
 * @param commits             commits linked to the fix; reserved
 * @param links               links to other bugs
 * @param commentsCount       number of comments
 * @param attachmentsCount    number of attachments
 * @param deadline            planned fix deadline
 * @param overdue             whether the deadline has passed
 * @param createdAt           creation timestamp
 * @param updatedAt           last update timestamp
 */
public record BugDetailsDto(
		UUID id,
		UUID projectId,
		String key,
		String title,
		String description,
		String stepsToReproduce,
		String environment,
		String expectedResult,
		String actualResult,
		BugStatus status,
		BugPriority priority,
		BugPriority recommendedPriority,
		BugResolution resolution,
		UserShortDto author,
		UserShortDto assignee,
		SoftwareComponentDto component,
		List<LabelDto> labels,
		FixPlanDto fixPlan,
		Object targetRelease,
		Object fixedInRelease,
		List<Object> commits,
		List<BugLinkDto> links,
		long commentsCount,
		long attachmentsCount,
		Instant deadline,
		boolean overdue,
		Instant createdAt,
		Instant updatedAt
) {
}
