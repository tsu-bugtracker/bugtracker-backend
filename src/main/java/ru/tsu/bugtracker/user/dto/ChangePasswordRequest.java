package ru.tsu.bugtracker.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for changing the current user's password.
 *
 * @param currentPassword current password
 * @param newPassword     new password
 */
public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		@NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
