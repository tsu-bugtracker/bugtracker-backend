package com.bugtracker.dto;

import com.bugtracker.domain.enums.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueUpdateDto {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    private String stepsToReproduce;

    @NotNull
    private IssuePriority priority;

    private UUID assigneeId;

    private UUID componentId;

    private UUID releaseId;

    private UUID fixReleaseId;

    private LocalDate deadline;

    private Set<UUID> tagIds;
}
