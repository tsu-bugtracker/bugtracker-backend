package ru.tsu.bugtracker.bug.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.bug.BugHistory;
import ru.tsu.bugtracker.common.dto.UserShortDto;

/**
 * Representation of a single bug history entry.
 *
 * @param id        history entry identifier
 * @param bugId     owning bug identifier
 * @param actor     user who made the change
 * @param fieldName name of the changed field
 * @param oldValue  previous value
 * @param newValue  new value
 * @param createdAt change timestamp
 */
public record BugHistoryEntryDto(
		UUID id,
		UUID bugId,
		UserShortDto actor,
		String fieldName,
		String oldValue,
		String newValue,
		OffsetDateTime createdAt
) {

	public static BugHistoryEntryDto from(BugHistory history) {
		return new BugHistoryEntryDto(
				history.getId(),
				history.getBug() != null ? history.getBug().getId() : null,
				history.getActor() != null ? UserShortDto.from(history.getActor()) : null,
				history.getFieldName(),
				history.getOldValue(),
				history.getNewValue(),
				history.getCreatedAt()
		);
	}

}
