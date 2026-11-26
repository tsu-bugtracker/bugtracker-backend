package ru.tsu.bugtracker.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a project.
 *
 * @param name           project name
 * @param description    free-form description
 * @param techStack      technologies used by the project
 * @param gitRepositoryUrl URL of the project's Git repository
 * @param startDate      planned start date
 * @param endDate        planned end date
 */
public record CreateProjectRequest(
		@NotBlank @Size(max = 150) String name,
		String description,
		@Size(max = 500) String techStack,
		String gitRepositoryUrl,
		LocalDate startDate,
		LocalDate endDate
) {
}
