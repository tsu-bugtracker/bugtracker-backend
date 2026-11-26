package ru.tsu.bugtracker.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserStatus;

/**
 * Public representation of a user's profile.
 *
 * @param id                user identifier
 * @param email             email address
 * @param fullName          display name
 * @param avatarUrl         URL of the profile picture
 * @param isAdmin           whether the user has administrator rights
 * @param emailVerified     whether the email address has been verified
 * @param status            current account status
 * @param canCreateProjects whether the user may create new projects
 * @param createdAt         registration timestamp
 */
public record UserProfileDto(
		UUID id,
		String email,
		String fullName,
		String avatarUrl,
		boolean isAdmin,
		boolean emailVerified,
		UserStatus status,
		boolean canCreateProjects,
		OffsetDateTime createdAt
) {

	public static UserProfileDto from(User user) {
		return new UserProfileDto(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getAvatarUrl(),
				user.isAdmin(),
				user.isEmailVerified(),
				user.getStatus(),
				user.isCanCreateProjects(),
				user.getCreatedAt()
		);
	}

}
