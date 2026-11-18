package com.bugtracker.mapper;

import com.bugtracker.domain.entity.IssueLink;
import com.bugtracker.dto.IssueLinkDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface IssueLinkMapper {

    @Mapping(target = "sourceIssueId", source = "sourceIssue.id")
    @Mapping(target = "targetIssueId", source = "targetIssue.id")
    IssueLinkDto toDto(IssueLink entity);

    List<IssueLinkDto> toDtoList(List<IssueLink> links);
}
