package ru.tsu.bugtracker.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ru.tsu.bugtracker.git.GitProvider;

/**
 * Payload for creating or updating a project git integration.
 *
 * @param repositoryUrl repository URL
 * @param provider      git hosting provider
 * @param accessToken   optional access token
 * @param defaultBranch optional default branch
 */
public record UpsertGitIntegrationRequest(
		@NotBlank @Size(max = 1024) String repositoryUrl,
		@NotNull GitProvider provider,
		String accessToken,
		@Size(max = 255) String defaultBranch
) {

}
