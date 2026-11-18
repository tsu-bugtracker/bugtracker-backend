package com.bugtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDto {

    private Long id;
    private String name;
    private String description;
    private String gitRepoUrl;
    private UserResponseDto createdBy;
    private boolean isActive;
    private boolean trackingActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
