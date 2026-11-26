package ru.tsu.bugtracker.bug.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for updating a comment.
 *
 * @param text new comment body
 */
public record UpdateCommentRequest(
		@NotBlank String text
) {
}
