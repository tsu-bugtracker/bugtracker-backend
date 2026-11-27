package ru.tsu.bugtracker.release.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a release.
 *
 * @param version     new release version
 * @param name        new release name
 * @param description new free-form description
 * @param plannedDate new planned release date
 */
public record UpdateReleaseRequest(
		@Size(max = 64) String version,
		@Size(max = 255) String name,
		String description,
		LocalDate plannedDate
) {
}
