package ru.tsu.bugtracker.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminRoleRequest(
		@NotNull Boolean isAdmin) {

}
