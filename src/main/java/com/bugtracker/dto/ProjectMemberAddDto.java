package com.bugtracker.dto;

import com.bugtracker.domain.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberAddDto {

    @NotNull
    private UUID userId;

    @NotNull
    private ProjectRole role;
}
