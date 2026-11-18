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

    private Long assigneeId;

    private Long componentId;

    private Long releaseId;

    private Long fixReleaseId;

    private LocalDate deadline;

    private Set<Long> tagIds;
}
