package ru.tsu.bugtracker.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.project.Label;

/**
 * Representation of a project label.
 *
 * @param id        label identifier
 * @param projectId owning project identifier
 * @param name      label name
 * @param color     optional color value
 * @param createdAt creation timestamp
 */
public record LabelDto(
		UUID id,
		UUID projectId,
		String name,
		String color,
		OffsetDateTime createdAt
) {

	public static LabelDto from(Label l) {
		return new LabelDto(
				l.getId(),
				l.getProject() != null ? l.getProject().getId() : null,
				l.getName(),
				l.getColor(),
				l.getCreatedAt()
		);
	}

}
