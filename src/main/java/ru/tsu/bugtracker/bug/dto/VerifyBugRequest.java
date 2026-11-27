package ru.tsu.bugtracker.bug.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for verifying a fixed bug.
 *
 * @param decision         verification decision
 * @param fixedInReleaseId release that contains the fix
 * @param comment          optional verification comment
 */
public record VerifyBugRequest(
		@NotNull VerifyDecision decision,
		UUID fixedInReleaseId,
		String comment
) {
}
