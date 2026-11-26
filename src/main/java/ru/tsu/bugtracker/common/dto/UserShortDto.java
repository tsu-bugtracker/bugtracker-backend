package ru.tsu.bugtracker.common.dto;

import java.util.UUID;

import ru.tsu.bugtracker.user.User;

/**
 * Compact user representation used in nested responses.
 *
 * @param id        user identifier
 * @param email     email address
 * @param fullName  full name
 * @param avatarUrl optional avatar URL
 */
public record UserShortDto(
		UUID id,
		String email,
		String fullName,
		String avatarUrl
) {

	public static UserShortDto from(User user) {
		return new UserShortDto(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getAvatarUrl()
		);
	}

}
