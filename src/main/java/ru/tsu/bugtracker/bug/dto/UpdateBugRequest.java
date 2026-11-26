package ru.tsu.bugtracker.bug.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Size;

/**
 * Payload for partially updating a bug. Fields left {@code null} are not changed.
 *
 * @param title            new short summary
 * @param description      new detailed description
 * @param stepsToReproduce new reproduction steps
 * @param environment      new environment description
 * @param expectedResult   new expected behaviour
 * @param actualResult     new actual behaviour
 * @param componentId      new affected software component
 * @param labelIds         identifiers of existing labels to attach
 * @param newLabels        names of labels to create and attach
 */
public record UpdateBugRequest(
		@Size(min = 1, max = 255) String title,
		String description,
		String stepsToReproduce,
		String environment,
		String expectedResult,
		String actualResult,
		UUID componentId,
		List<UUID> labelIds,
		List<String> newLabels
) {
}
