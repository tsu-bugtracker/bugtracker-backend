package ru.tsu.bugtracker.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.user.UserStatus;

public record AdminUserSummaryDto(
		UUID id,
		String email,
		String fullName,
		boolean isAdmin,
		boolean emailVerified,
		UserStatus status,
		int projectsCount,
		OffsetDateTime createdAt) {

}
