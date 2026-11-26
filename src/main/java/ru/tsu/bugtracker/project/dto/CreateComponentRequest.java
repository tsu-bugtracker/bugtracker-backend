package ru.tsu.bugtracker.project.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a software component.
 *
 * @param name            component name
 * @param description     optional description
 * @param ownerDeveloperId optional owner developer identifier
 */
public record CreateComponentRequest(
		@NotBlank @Size(max = 150) String name,
		String description,
		UUID ownerDeveloperId
) {

}
