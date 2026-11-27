package ru.tsu.bugtracker.release.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a release.
 *
 * @param version     release version
 * @param name        release name
 * @param description free-form description
 * @param plannedDate planned release date
 * @param bugIds      bugs to associate with the release
 */
public record CreateReleaseRequest(
		@NotBlank @Size(max = 64) String version,
		@Size(max = 255) String name,
		String description,
		LocalDate plannedDate,
		List<UUID> bugIds
) {
}
