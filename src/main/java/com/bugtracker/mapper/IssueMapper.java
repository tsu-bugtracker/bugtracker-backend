package com.bugtracker.mapper;

import com.bugtracker.domain.entity.Issue;
import com.bugtracker.dto.IssueResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {UserMapper.class, ComponentMapper.class, ReleaseMapper.class, TagMapper.class}
)
public interface IssueMapper {

    @Mapping(target = "projectId", source = "project.id")
    IssueResponseDto toDto(Issue entity);

    List<IssueResponseDto> toDtoList(List<Issue> issues);
}
