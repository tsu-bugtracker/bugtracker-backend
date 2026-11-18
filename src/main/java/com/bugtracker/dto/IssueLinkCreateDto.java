package com.bugtracker.dto;

import com.bugtracker.domain.enums.LinkType;
import jakarta.validation.constraints.NotNull;
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
public class IssueLinkCreateDto {

    @NotNull
    private Long targetIssueId;

    @NotNull
    private LinkType linkType;
}
