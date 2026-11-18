package com.bugtracker.service;

import com.bugtracker.domain.entity.Project;
import com.bugtracker.domain.entity.ProjectMember;
import com.bugtracker.domain.entity.User;
import com.bugtracker.domain.enums.ProjectRole;
import com.bugtracker.dto.ProjectMemberAddDto;
import com.bugtracker.dto.ProjectMemberResponseDto;
import com.bugtracker.dto.ProjectMemberUpdateDto;
import com.bugtracker.exception.BusinessRuleException;
import com.bugtracker.exception.ConflictException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.ProjectMemberMapper;
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
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectService projectService;

    @Transactional
    public ProjectMemberResponseDto addMember(UUID projectId, ProjectMemberAddDto dto) {
        Project project = findProject(projectId);
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + dto.getUserId() + " not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, dto.getUserId())) {
            throw new ConflictException(
                    "User with id " + dto.getUserId() + " is already a member of project " + projectId);
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(dto.getRole())
                .joinedAt(LocalDateTime.now())
                .build();

        ProjectMember savedMember = projectMemberRepository.save(member);

        projectService.updateTrackingStatus(project);

        return projectMemberMapper.toDto(savedMember);
    }

    public List<ProjectMemberResponseDto> getMembers(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id " + projectId + " not found");
        }
        return projectMemberMapper.toDtoList(projectMemberRepository.findAllByProjectId(projectId));
    }

    @Transactional
    public ProjectMemberResponseDto updateMemberRole(UUID projectId, UUID userId, ProjectMemberUpdateDto dto) {
        ProjectMember member = findMember(projectId, userId);

        member.setRole(dto.getRole());
        ProjectMember savedMember = projectMemberRepository.save(member);

        projectService.updateTrackingStatus(member.getProject());

        return projectMemberMapper.toDto(savedMember);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        ProjectMember member = findMember(projectId, userId);

        if (member.getRole() == ProjectRole.TEAM_LEAD
                && projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.TEAM_LEAD) <= 1) {
            throw new BusinessRuleException("Cannot remove the only team lead from project");
        }

        projectMemberRepository.delete(member);

        projectService.updateTrackingStatus(member.getProject());
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project with id " + projectId + " not found"));
    }

    private ProjectMember findMember(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " is not a member of project " + projectId));
    }
}
