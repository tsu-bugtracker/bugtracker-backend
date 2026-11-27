package ru.tsu.bugtracker.release.dto;

/**
 * Request payload for publishing a release.
 *
 * @param releaseNotes notes published with the release
 */
public record PublishReleaseRequest(
		String releaseNotes
) {
}
