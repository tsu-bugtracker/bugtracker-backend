package ru.tsu.bugtracker.planning.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ru.tsu.bugtracker.bug.BugPriority;

/**
 * Payload for creating or updating a bug fix plan.
 *
 * @param priority             priority set by the plan
 * @param priorityChangeReason reason for changing the priority
 * @param deadline             planned fix deadline
 * @param assigneeId           user assigned to fix the bug
 * @param targetReleaseId      release the fix is planned for
 * @param rationale            justification for the plan
 */
public record FixPlanRequest(
		@NotNull BugPriority priority,
		String priorityChangeReason,
		OffsetDateTime deadline,
		UUID assigneeId,
		UUID targetReleaseId,
		@NotBlank String rationale
) {
}
