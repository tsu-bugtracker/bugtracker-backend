package ru.tsu.bugtracker.git.dto;

import java.time.Instant;
import java.util.UUID;

import ru.tsu.bugtracker.git.GitIntegration;
import ru.tsu.bugtracker.git.GitProvider;

/**
 * Representation of a project git integration.
 *
 * @param id             integration identifier
 * @param projectId      identifier of the linked project
 * @param repositoryUrl  repository URL
 * @param provider       git hosting provider
 * @param defaultBranch  default branch used for sync
 * @param hasAccessToken whether an access token is configured
 * @param connected      whether the integration is connected
 * @param lastSyncedAt   last successful sync timestamp
 */
public record GitIntegrationResponse(
		UUID id,
		UUID projectId,
		String repositoryUrl,
		GitProvider provider,
		String defaultBranch,
		boolean hasAccessToken,
		boolean connected,
		Instant lastSyncedAt
) {

	public static GitIntegrationResponse from(GitIntegration entity) {
		if (entity == null) {
			return null;
		}
		String accessToken = entity.getAccessToken();
		return new GitIntegrationResponse(
				entity.getId(),
				entity.getProject() != null ? entity.getProject().getId() : null,
				entity.getRepositoryUrl(),
				entity.getProvider(),
				entity.getDefaultBranch(),
				accessToken != null && !accessToken.isBlank(),
				entity.isConnected(),
				entity.getLastSyncedAt()
		);
	}

}
