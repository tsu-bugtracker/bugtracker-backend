package ru.tsu.bugtracker.project.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload for partially updating a project label. {@code null} fields are left unchanged.
 *
 * @param name  new label name, if provided
 * @param color new color value, if provided
 */
public record UpdateLabelRequest(
		@Size(min = 1, max = 100) String name,
		@Size(max = 32) String color
) {

}
