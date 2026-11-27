package ru.tsu.bugtracker.git.dto;

import java.time.Instant;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.git.BugLinkedCommit;
import ru.tsu.bugtracker.git.GitCommitType;

/**
 * Representation of a commit or merge request linked to a bug.
 *
 * @param id              link identifier
 * @param bugId           identifier of the linked bug
 * @param type            commit type
 * @param commitHash      commit hash
 * @param mergeRequestUrl merge request URL
 * @param authorName      author name
 * @param committedAt     commit timestamp
 * @param linkedBy        user who linked the commit
 * @param linkedAt        link timestamp
 */
public record LinkedCommitDto(
		UUID id,
		UUID bugId,
		GitCommitType type,
		String commitHash,
		String mergeRequestUrl,
		String authorName,
		Instant committedAt,
		UserShortDto linkedBy,
		Instant linkedAt
) {

	public static LinkedCommitDto from(BugLinkedCommit entity) {
		if (entity == null) {
			return null;
		}
		return new LinkedCommitDto(
				entity.getId(),
				entity.getBug() != null ? entity.getBug().getId() : null,
				entity.getType(),
				entity.getCommitHash(),
				entity.getMergeRequestUrl(),
				entity.getAuthorName(),
				entity.getCommittedAt(),
				entity.getLinkedBy() != null ? UserShortDto.from(entity.getLinkedBy()) : null,
				entity.getLinkedAt()
		);
	}

}
