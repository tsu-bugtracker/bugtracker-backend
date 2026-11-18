package com.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCreateDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @Size(max = 255)
    private String gitRepoUrl;

    @NotNull
    private Long createdById;
}
