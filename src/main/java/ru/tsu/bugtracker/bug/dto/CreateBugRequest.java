package ru.tsu.bugtracker.bug.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ru.tsu.bugtracker.bug.BugPriority;

/**
 * Payload for creating a bug.
 *
 * @param title               short summary of the bug
 * @param description         detailed description
 * @param stepsToReproduce    steps needed to reproduce the bug
 * @param environment         environment where the bug was observed
 * @param expectedResult      expected behaviour
 * @param actualResult        actual behaviour
 * @param recommendedPriority priority recommended by the reporter
 * @param componentId         affected software component
 * @param labelIds            identifiers of existing labels to attach
 * @param newLabels           names of labels to create and attach
 */
public record CreateBugRequest(
		@NotBlank @Size(max = 255) String title,
		@NotBlank String description,
		@NotBlank String stepsToReproduce,
		@NotBlank String environment,
		String expectedResult,
		String actualResult,
		@NotNull BugPriority recommendedPriority,
		UUID componentId,
		List<UUID> labelIds,
		List<String> newLabels
) {
}
