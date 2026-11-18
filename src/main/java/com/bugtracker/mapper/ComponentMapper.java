package com.bugtracker.mapper;

import com.bugtracker.domain.entity.Component;
import com.bugtracker.dto.ComponentCreateDto;
import com.bugtracker.dto.ComponentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ComponentMapper {

    Component toEntity(ComponentCreateDto dto);

    @Mapping(target = "projectId", source = "project.id")
    ComponentDto toDto(Component entity);

    List<ComponentDto> toDtoList(List<Component> components);
}
