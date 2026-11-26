package ru.tsu.bugtracker.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;

/**
 * A single audit log entry returned to clients.
 *
 * @param id         entry identifier
 * @param actor      user who performed the action, or {@code null} for system actions
 * @param action     action name
 * @param targetType type of the affected entity
 * @param targetId   identifier of the affected entity, or {@code null}
 * @param details    free-form action details, or {@code null}
 * @param createdAt  creation timestamp
 */
public record AuditLogEntryDto(
		UUID id,
		UserShortDto actor,
		String action,
		String targetType,
		UUID targetId,
		Map<String, Object> details,
		OffsetDateTime createdAt
) {
}
