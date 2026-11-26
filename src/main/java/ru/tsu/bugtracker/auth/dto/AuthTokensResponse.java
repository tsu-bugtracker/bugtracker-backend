package ru.tsu.bugtracker.auth.dto;

import ru.tsu.bugtracker.user.dto.UserProfileDto;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserProfileDto user
) {
}
