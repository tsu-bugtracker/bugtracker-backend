package com.bugtracker.dto;

import com.bugtracker.domain.enums.ProjectRole;
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
public class ProjectMemberResponseDto {

    private Long id;
    private Long projectId;
    private UserResponseDto user;
    private ProjectRole role;
    private LocalDateTime joinedAt;
}
