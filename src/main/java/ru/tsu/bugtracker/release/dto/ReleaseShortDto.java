package ru.tsu.bugtracker.release.dto;

import java.util.UUID;

import ru.tsu.bugtracker.release.Release;
import ru.tsu.bugtracker.release.ReleaseStatus;

/**
 * Compact release representation used in nested payloads.
 *
 * @param id      release identifier
 * @param version release version
 * @param name    release name
 * @param status  lifecycle status
 */
public record ReleaseShortDto(
		UUID id,
		String version,
		String name,
		ReleaseStatus status
) {

	public static ReleaseShortDto from(Release release) {
		if (release == null) {
			return null;
		}
		return new ReleaseShortDto(
				release.getId(),
				release.getVersion(),
				release.getName(),
				release.getStatus());
	}
}
