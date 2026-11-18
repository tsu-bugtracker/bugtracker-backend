package com.bugtracker.dto;

import com.bugtracker.domain.enums.IssueResolution;
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
public class IssueCloseDto {

    @NotNull
    private IssueResolution resolution;

    private UUID fixReleaseId;

    private String comment;
}
