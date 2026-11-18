package com.bugtracker.dto;

import com.bugtracker.domain.enums.ProjectRole;
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
public class ProjectMemberResponseDto {

    private UUID id;
    private UUID projectId;
    private UserResponseDto user;
    private ProjectRole role;
    private LocalDateTime joinedAt;
}
