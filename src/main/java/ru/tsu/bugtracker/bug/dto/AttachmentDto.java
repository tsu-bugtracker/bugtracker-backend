package ru.tsu.bugtracker.bug.dto;

import java.time.Instant;
import java.util.UUID;

import ru.tsu.bugtracker.bug.Attachment;
import ru.tsu.bugtracker.common.dto.UserShortDto;

/**
 * Representation of a file attached to a bug or comment.
 *
 * @param id           attachment identifier
 * @param bugId        owning bug identifier
 * @param commentId    owning comment identifier, if any
 * @param fileName     original file name
 * @param contentType  MIME content type
 * @param sizeBytes    file size in bytes
 * @param uploadedBy   user who uploaded the file
 * @param createdAt    upload timestamp
 */
public record AttachmentDto(
		UUID id,
		UUID bugId,
		UUID commentId,
		String fileName,
		String contentType,
		long sizeBytes,
		UserShortDto uploadedBy,
		Instant createdAt
) {

	public static AttachmentDto from(Attachment attachment) {
		return new AttachmentDto(
				attachment.getId(),
				attachment.getBug() != null ? attachment.getBug().getId() : null,
				attachment.getComment() != null ? attachment.getComment().getId() : null,
				attachment.getFileName(),
				attachment.getContentType(),
				attachment.getSizeBytes() != null ? attachment.getSizeBytes() : 0L,
				attachment.getUploadedBy() != null ? UserShortDto.from(attachment.getUploadedBy()) : null,
				attachment.getCreatedAt()
		);
	}

}
