package ru.tsu.bugtracker.admin.dto;

import jakarta.validation.constraints.Size;

public record DeactivateUserRequest(
		@Size(max = 500) String reason) {

}
