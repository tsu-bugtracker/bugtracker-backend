package ru.tsu.bugtracker.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.project.SoftwareComponent;

/**
 * Representation of a software component belonging to a project.
 *
 * @param id             component identifier
 * @param projectId      owning project identifier
 * @param name           component name
 * @param description    optional description
 * @param ownerDeveloper optional developer responsible for the component
 * @param createdAt      creation timestamp
 */
public record SoftwareComponentDto(
		UUID id,
		UUID projectId,
		String name,
		String description,
		UserShortDto ownerDeveloper,
		OffsetDateTime createdAt
) {

	public static SoftwareComponentDto from(SoftwareComponent c) {
		return new SoftwareComponentDto(
				c.getId(),
				c.getProject() != null ? c.getProject().getId() : null,
				c.getName(),
				c.getDescription(),
				c.getOwnerDeveloper() != null ? UserShortDto.from(c.getOwnerDeveloper()) : null,
				c.getCreatedAt()
		);
	}

}
