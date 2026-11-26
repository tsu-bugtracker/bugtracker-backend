package ru.tsu.bugtracker.project.dto;

import java.util.UUID;

import jakarta.validation.constraints.Size;

/**
 * Payload for partially updating a software component. {@code null} fields are left unchanged.
 *
 * @param name             new component name, if provided
 * @param description      new description, if provided
 * @param ownerDeveloperId new owner developer identifier, if provided
 */
public record UpdateComponentRequest(
		@Size(min = 1, max = 150) String name,
		String description,
		UUID ownerDeveloperId
) {

}
