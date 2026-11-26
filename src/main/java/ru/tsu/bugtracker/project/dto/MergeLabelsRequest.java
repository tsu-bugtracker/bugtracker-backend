package ru.tsu.bugtracker.project.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for merging several labels into a single target label.
 *
 * @param sourceLabelIds labels to merge and remove
 * @param targetLabelId  label that receives the merged assignments
 */
public record MergeLabelsRequest(
		@NotEmpty List<UUID> sourceLabelIds,
		@NotNull UUID targetLabelId
) {

}
