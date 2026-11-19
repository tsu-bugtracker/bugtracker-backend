package com.bugtracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDto {

    private UUID id;
    private String name;
    private String description;
    private String gitRepoUrl;
    private UserResponseDto createdBy;
    @JsonProperty("isActive")
    private boolean isActive;
    private boolean trackingActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }
}
