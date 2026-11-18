package com.bugtracker.mapper;

import com.bugtracker.domain.entity.Project;
import com.bugtracker.dto.ProjectCreateDto;
import com.bugtracker.dto.ProjectResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = UserMapper.class
)
public interface ProjectMapper {

    Project toEntity(ProjectCreateDto dto);

    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "isActive", source = "active")
    ProjectResponseDto toDto(Project entity);
}
