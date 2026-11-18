package com.bugtracker.service;

import com.bugtracker.domain.entity.User;
import com.bugtracker.dto.UserCreateDto;
import com.bugtracker.dto.UserResponseDto;
import com.bugtracker.exception.ConflictException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.UserMapper;
import com.bugtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto createUser(UserCreateDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ConflictException("User with username '" + dto.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("User with email '" + dto.getEmail() + "' already exists");
        }

        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return userMapper.toDto(user);
    }

    public List<UserResponseDto> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }
}
