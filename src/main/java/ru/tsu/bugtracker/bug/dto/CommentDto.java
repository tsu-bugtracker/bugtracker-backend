package ru.tsu.bugtracker.bug.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;

/**
 * Representation of a comment on a bug.
 *
 * @param id          comment identifier
 * @param bugId       owning bug identifier
 * @param author      user who wrote the comment
 * @param text        comment body
 * @param attachments files attached to the comment
 * @param createdAt   creation timestamp
 * @param updatedAt   last update timestamp
 */
public record CommentDto(
		UUID id,
		UUID bugId,
		UserShortDto author,
		String text,
		List<AttachmentDto> attachments,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
