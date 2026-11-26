package ru.tsu.bugtracker.user.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for updating the current user's profile.
 *
 * @param fullName  new display name
 * @param avatarUrl new avatar URL
 */
public record UpdateProfileRequest(
		@Size(min = 1, max = 255) String fullName,
		@Size(max = 1024) String avatarUrl
) {
}
