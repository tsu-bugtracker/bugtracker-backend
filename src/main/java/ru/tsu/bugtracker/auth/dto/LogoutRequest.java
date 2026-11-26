package ru.tsu.bugtracker.auth.dto;

public record LogoutRequest(
        String refreshToken
) {
}
