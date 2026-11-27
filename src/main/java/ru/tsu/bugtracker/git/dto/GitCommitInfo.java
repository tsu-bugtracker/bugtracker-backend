package ru.tsu.bugtracker.git.dto;

import java.time.Instant;
import java.util.UUID;

import ru.tsu.bugtracker.git.GitCommit;
import ru.tsu.bugtracker.git.GitCommitType;

/**
 * Representation of a synchronized commit or merge request.
 *
 * @param id              commit identifier
 * @param type            commit type
 * @param commitHash      commit hash
 * @param mergeRequestUrl merge request URL
 * @param title           commit title
 * @param authorName      author name
 * @param committedAt     commit timestamp
 * @param branch          branch name
 */
public record GitCommitInfo(
		UUID id,
		GitCommitType type,
		String commitHash,
		String mergeRequestUrl,
		String title,
		String authorName,
		Instant committedAt,
		String branch
) {

	public static GitCommitInfo from(GitCommit entity) {
		if (entity == null) {
			return null;
		}
		return new GitCommitInfo(
				entity.getId(),
				entity.getType(),
				entity.getCommitHash(),
				entity.getMergeRequestUrl(),
				entity.getTitle(),
				entity.getAuthorName(),
				entity.getCommittedAt(),
				entity.getBranch()
		);
	}

}
