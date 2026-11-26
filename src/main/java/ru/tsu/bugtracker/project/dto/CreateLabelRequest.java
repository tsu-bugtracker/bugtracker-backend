package ru.tsu.bugtracker.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a project label.
 *
 * @param name  label name
 * @param color optional color value
 */
public record CreateLabelRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 32) String color
) {

}
