package ru.tsu.bugtracker.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import ru.tsu.bugtracker.user.UserStatus;

public record AdminUserDetailsDto(
		UUID id,
		String email,
		String fullName,
		String avatarUrl,
		boolean isAdmin,
		boolean emailVerified,
		UserStatus status,
		boolean canCreateProjects,
		String blockedReason,
		List<UserProjectMembershipDto> projects,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

}
