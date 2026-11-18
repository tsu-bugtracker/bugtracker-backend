package com.bugtracker.mapper;

import com.bugtracker.domain.entity.Tag;
import com.bugtracker.dto.TagCreateDto;
import com.bugtracker.dto.TagDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Set;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TagMapper {

    Tag toEntity(TagCreateDto dto);

    @Mapping(target = "projectId", source = "project.id")
    TagDto toDto(Tag entity);

    Set<TagDto> toDtoSet(Set<Tag> tags);
}
