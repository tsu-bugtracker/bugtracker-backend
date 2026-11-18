package com.bugtracker.dto;

import com.bugtracker.domain.enums.LinkType;
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
public class IssueLinkDto {

    private Long id;
    private Long sourceIssueId;
    private Long targetIssueId;
    private LinkType linkType;
}
