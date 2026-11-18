package com.bugtracker.dto;

import com.bugtracker.domain.enums.IssuePriority;
import com.bugtracker.domain.enums.IssueResolution;
import com.bugtracker.domain.enums.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueResponseDto {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String stepsToReproduce;
    private IssuePriority priority;
    private IssueStatus status;
    private IssueResolution resolution;
    private UserResponseDto reporter;
    private UserResponseDto assignee;
    private ComponentDto component;
    private ReleaseDto release;
    private ReleaseDto fixRelease;
    private LocalDate deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<TagDto> tags;
}
