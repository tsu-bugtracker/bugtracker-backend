package ru.tsu.bugtracker.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a project.
 *
 * @param name        new project name
 * @param description new free-form description
 * @param techStack   new technologies used by the project
 * @param startDate   planned start date
 * @param endDate     planned end date
 */
public record UpdateProjectRequest(
		@Size(min = 1, max = 150) String name,
		String description,
		@Size(max = 500) String techStack,
		LocalDate startDate,
		LocalDate endDate
) {
}
