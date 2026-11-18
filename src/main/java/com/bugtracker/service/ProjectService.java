package com.bugtracker.service;

import com.bugtracker.domain.entity.Project;
import com.bugtracker.domain.entity.ProjectMember;
import com.bugtracker.domain.entity.User;
import com.bugtracker.domain.enums.ProjectRole;
import com.bugtracker.dto.ProjectCreateDto;
import com.bugtracker.dto.ProjectResponseDto;
import com.bugtracker.dto.ProjectUpdateDto;
import com.bugtracker.exception.ConflictException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.ProjectMapper;
import com.bugtracker.repository.ProjectMemberRepository;
import com.bugtracker.repository.ProjectRepository;
import com.bugtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponseDto createProject(ProjectCreateDto dto) {
        if (projectRepository.existsByName(dto.getName())) {
            throw new ConflictException("Project with name '" + dto.getName() + "' already exists");
        }

        User creator = userRepository.findById(dto.getCreatedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + dto.getCreatedById() + " not found"));

        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .gitRepoUrl(dto.getGitRepoUrl())
                .createdBy(creator)
                .isActive(true)
                .trackingActive(false)
                .build();

        Project savedProject = projectRepository.save(project);

        ProjectMember teamLead = ProjectMember.builder()
                .project(savedProject)
                .user(creator)
                .role(ProjectRole.TEAM_LEAD)
                .joinedAt(LocalDateTime.now())
                .build();
        projectMemberRepository.save(teamLead);

        updateTrackingStatus(savedProject);

        return projectMapper.toDto(savedProject);
    }

    public ProjectResponseDto getProjectById(UUID id) {
        Project project = findProject(id);
        return projectMapper.toDto(project);
    }

    public List<ProjectResponseDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Transactional
    public ProjectResponseDto updateProject(UUID id, ProjectUpdateDto dto) {
        Project project = findProject(id);

        if (!project.getName().equals(dto.getName()) && projectRepository.existsByName(dto.getName())) {
            throw new ConflictException("Project with name '" + dto.getName() + "' already exists");
        }

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setGitRepoUrl(dto.getGitRepoUrl());
        if (dto.getIsActive() != null) {
            project.setActive(dto.getIsActive());
        }

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    @Transactional
    public void deleteProject(UUID id) {
        Project project = findProject(id);
        project.setActive(false);
        projectRepository.save(project);
    }

    /**
     * Recalculates {@code trackingActive} according to the minimum team composition rule:
     * a project can only be tracked when it has at least one team lead and at least one tester.
     */
    @Transactional
    public void updateTrackingStatus(Project project) {
        boolean hasTeamLead = projectMemberRepository.existsByProjectIdAndRole(
                project.getId(), ProjectRole.TEAM_LEAD);
        boolean hasTester = projectMemberRepository.existsByProjectIdAndRole(
                project.getId(), ProjectRole.TESTER);

        project.setTrackingActive(hasTeamLead && hasTester);
        projectRepository.save(project);
    }

    private Project findProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " not found"));
    }
}
