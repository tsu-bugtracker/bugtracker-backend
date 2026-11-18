package com.bugtracker.mapper;

import com.bugtracker.domain.entity.ProjectMember;
import com.bugtracker.dto.ProjectMemberResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = UserMapper.class
)
public interface ProjectMemberMapper {

    @Mapping(target = "projectId", source = "project.id")
    ProjectMemberResponseDto toDto(ProjectMember entity);

    List<ProjectMemberResponseDto> toDtoList(List<ProjectMember> members);
}
