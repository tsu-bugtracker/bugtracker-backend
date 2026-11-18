package com.bugtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseDto {

    private Long id;
    private Long projectId;
    private String versionName;
    private LocalDate releaseDate;
    private boolean isReleased;
}
