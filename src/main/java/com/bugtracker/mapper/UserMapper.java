package com.bugtracker.mapper;

import com.bugtracker.domain.entity.User;
import com.bugtracker.dto.UserCreateDto;
import com.bugtracker.dto.UserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    User toEntity(UserCreateDto dto);

    UserResponseDto toDto(User entity);

    List<UserResponseDto> toDtoList(List<User> users);
}
