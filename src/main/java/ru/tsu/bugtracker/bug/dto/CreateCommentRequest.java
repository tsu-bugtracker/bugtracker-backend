package ru.tsu.bugtracker.bug.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for creating a comment.
 *
 * @param text comment body
 */
public record CreateCommentRequest(
		@NotBlank String text
) {
}
