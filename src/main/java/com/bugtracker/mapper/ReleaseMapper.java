package com.bugtracker.mapper;

import com.bugtracker.domain.entity.Release;
import com.bugtracker.dto.ReleaseCreateDto;
import com.bugtracker.dto.ReleaseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReleaseMapper {

    @Mapping(target = "isReleased", source = "released")
    Release toEntity(ReleaseCreateDto dto);

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "isReleased", source = "released")
    ReleaseDto toDto(Release entity);

    List<ReleaseDto> toDtoList(List<Release> releases);
}
