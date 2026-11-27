package ru.tsu.bugtracker.planning.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.planning.FixPlan;

/**
 * Representation of a bug fix plan.
 *
 * @param id                   fix plan identifier
 * @param priority             priority set by the plan
 * @param priorityChangeReason reason for changing the priority
 * @param deadline             planned fix deadline
 * @param rationale            justification for the plan
 * @param targetRelease        release the fix is planned for; reserved
 * @param plannedBy            user who created the plan
 * @param updatedAt            last update timestamp
 */
public record FixPlanDto(
		UUID id,
		BugPriority priority,
		String priorityChangeReason,
		OffsetDateTime deadline,
		String rationale,
		Object targetRelease,
		UserShortDto plannedBy,
		OffsetDateTime updatedAt
) {

	public static FixPlanDto from(FixPlan plan) {
		return new FixPlanDto(
				plan.getId(),
				plan.getPriority(),
				plan.getPriorityChangeReason(),
				plan.getDeadline(),
				plan.getRationale(),
				null,
				plan.getPlannedBy() != null ? UserShortDto.from(plan.getPlannedBy()) : null,
				plan.getUpdatedAt()
		);
	}

}
