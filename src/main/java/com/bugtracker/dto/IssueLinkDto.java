package com.bugtracker.dto;

import com.bugtracker.domain.enums.LinkType;
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
public class IssueLinkDto {

    private UUID id;
    private UUID sourceIssueId;
    private UUID targetIssueId;
    private LinkType linkType;
}
