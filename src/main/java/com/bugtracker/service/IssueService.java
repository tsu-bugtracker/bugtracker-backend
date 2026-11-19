package com.bugtracker.service;

import com.bugtracker.domain.entity.Component;
import com.bugtracker.domain.entity.Issue;
import com.bugtracker.domain.entity.Project;
import com.bugtracker.domain.entity.ProjectMember;
import com.bugtracker.domain.entity.Release;
import com.bugtracker.domain.entity.Tag;
import com.bugtracker.domain.enums.IssuePriority;
import com.bugtracker.domain.enums.IssueResolution;
import com.bugtracker.domain.enums.IssueStatus;
import com.bugtracker.domain.enums.ProjectRole;
import com.bugtracker.dto.IssueCloseDto;
import com.bugtracker.dto.IssueCreateDto;
import com.bugtracker.dto.IssueResponseDto;
import com.bugtracker.dto.IssueStatusTransitionDto;
import com.bugtracker.dto.IssueUpdateDto;
import com.bugtracker.exception.BusinessRuleException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.IssueMapper;
import com.bugtracker.repository.ComponentRepository;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.ProjectMemberRepository;
import com.bugtracker.repository.ProjectRepository;
import com.bugtracker.repository.ReleaseRepository;
import com.bugtracker.repository.TagRepository;
import com.bugtracker.specification.IssueSpecification;
import com.bugtracker.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;
    private final ComponentRepository componentRepository;
    private final ReleaseRepository releaseRepository;
    private final TagRepository tagRepository;
    private final IssueMapper issueMapper;

    @Transactional
    public IssueResponseDto createIssue(UUID projectId, IssueCreateDto dto) {
        Project project = findActiveProject(projectId);

        if (!project.isTrackingActive()) {
            throw new BusinessRuleException(
                    "Bug tracking is disabled for this project: team must have at least one team lead and one tester");
        }

        ProjectMember reporter = projectMemberRepository
                .findByProjectIdAndUserId(projectId, dto.getReporterId())
                .filter(member -> member.getRole() == ProjectRole.TESTER)
                .orElseThrow(() -> new BusinessRuleException(
                        "Only a project member with TESTER role can report an issue"));

        Issue issue = Issue.builder()
                .project(project)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .stepsToReproduce(dto.getStepsToReproduce())
                .priority(dto.getPriority())
                .status(IssueStatus.NEW)
                .resolution(null)
                .reporter(reporter.getUser())
                .assignee(null)
                .component(resolveComponent(projectId, dto.getComponentId()))
                .release(resolveRelease(projectId, dto.getReleaseId()))
                .deadline(dto.getDeadline())
                .tags(resolveTags(projectId, dto.getTagIds()))
                .build();

        return issueMapper.toDto(issueRepository.save(issue));
    }

    public IssueResponseDto getIssueById(UUID projectId, UUID issueId) {
        return issueMapper.toDto(findIssue(projectId, issueId));
    }

    public Page<IssueResponseDto> getIssues(UUID projectId,
                                            String search,
                                            IssueStatus status,
                                            IssuePriority priority,
                                            UUID assigneeId,
                                            UUID reporterId,
                                            UUID componentId,
                                            UUID releaseId,
                                            Pageable pageable) {
        Pageable cleanPageable = PaginationUtils.sanitizePageable(
                pageable, Sort.by(Sort.Direction.DESC, "createdAt"));
        return issueRepository
                .findAll(IssueSpecification.filter(projectId, search, status, priority, assigneeId,
                        reporterId, componentId, releaseId), cleanPageable)
                .map(issueMapper::toDto);
    }

    @Transactional
    public IssueResponseDto updateIssue(UUID projectId, UUID issueId, IssueUpdateDto dto) {
        Issue issue = findIssue(projectId, issueId);

        issue.setTitle(dto.getTitle());
        issue.setDescription(dto.getDescription());
        issue.setStepsToReproduce(dto.getStepsToReproduce());
        issue.setPriority(dto.getPriority());
        issue.setDeadline(dto.getDeadline());

        if (dto.getAssigneeId() != null) {
            ProjectMember assignee = projectMemberRepository
                    .findByProjectIdAndUserId(projectId, dto.getAssigneeId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Issue assignee must be a project member with DEVELOPER or TEAM_LEAD role"));

            if (assignee.getRole() != ProjectRole.DEVELOPER && assignee.getRole() != ProjectRole.TEAM_LEAD) {
                throw new BusinessRuleException(
                        "Issue assignee must be a project member with DEVELOPER or TEAM_LEAD role");
            }

            issue.setAssignee(assignee.getUser());
        }

        issue.setComponent(resolveComponent(projectId, dto.getComponentId()));
        issue.setRelease(resolveRelease(projectId, dto.getReleaseId()));
        issue.setFixRelease(resolveRelease(projectId, dto.getFixReleaseId()));

        if (dto.getTagIds() != null) {
            issue.setTags(resolveTags(projectId, dto.getTagIds()));
        }

        return issueMapper.toDto(issueRepository.save(issue));
    }

    @Transactional
    public IssueResponseDto changeStatus(UUID projectId, UUID issueId, UUID userId,
                                         IssueStatusTransitionDto dto) {
        Issue issue = findIssue(projectId, issueId);
        ProjectMember member = findMember(projectId, userId);

        IssueStatus currentStatus = issue.getStatus();
        IssueStatus targetStatus = dto.getStatus();

        if (currentStatus == IssueStatus.NEW && targetStatus == IssueStatus.PLANNED) {
            requireAnyRole(member,
                    "Only a project member with TEAM_LEAD role can plan an issue",
                    ProjectRole.TEAM_LEAD);
        } else if ((currentStatus == IssueStatus.NEW || currentStatus == IssueStatus.PLANNED)
                && targetStatus == IssueStatus.IN_PROGRESS) {
            requireAnyRole(member,
                    "Only a project member with DEVELOPER or TEAM_LEAD role can start work on an issue",
                    ProjectRole.DEVELOPER, ProjectRole.TEAM_LEAD);

            if (issue.getAssignee() == null && member.getRole() == ProjectRole.DEVELOPER) {
                issue.setAssignee(member.getUser());
            }
        } else if (currentStatus == IssueStatus.IN_PROGRESS
                && targetStatus == IssueStatus.REQUIRES_CLARIFICATION) {
            if (!isAssignee(issue, userId) && member.getRole() != ProjectRole.TEAM_LEAD) {
                throw new BusinessRuleException(
                        "Only the current assignee or a project member with TEAM_LEAD role can request clarification");
            }
        } else if (currentStatus == IssueStatus.REQUIRES_CLARIFICATION
                && targetStatus == IssueStatus.IN_PROGRESS) {
            if (member.getRole() != ProjectRole.TESTER
                    && member.getRole() != ProjectRole.TEAM_LEAD
                    && !isReporter(issue, userId)) {
                throw new BusinessRuleException(
                        "Only the reporter, a TESTER or a TEAM_LEAD can resume work on an issue");
            }
        } else if (currentStatus == IssueStatus.IN_PROGRESS && targetStatus == IssueStatus.IN_REVIEW) {
            if (!isAssignee(issue, userId) && member.getRole() != ProjectRole.DEVELOPER) {
                throw new BusinessRuleException(
                        "Only the current assignee or a project member with DEVELOPER role can submit an issue for review");
            }
        } else if (currentStatus == IssueStatus.IN_REVIEW && targetStatus == IssueStatus.IN_PROGRESS) {
            requireAnyRole(member,
                    "Only a project member with TESTER or TEAM_LEAD role can return an issue to work",
                    ProjectRole.TESTER, ProjectRole.TEAM_LEAD);
        } else {
            throw new BusinessRuleException(
                    "Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        issue.setStatus(targetStatus);
        return issueMapper.toDto(issueRepository.save(issue));
    }

    @Transactional
    public IssueResponseDto closeIssue(UUID projectId, UUID issueId, UUID userId, IssueCloseDto dto) {
        Issue issue = findIssue(projectId, issueId);
        ProjectMember member = findMember(projectId, userId);

        requireAnyRole(member,
                "Only a project member with TESTER or TEAM_LEAD role can close an issue",
                ProjectRole.TESTER, ProjectRole.TEAM_LEAD);

        IssueStatus currentStatus = issue.getStatus();
        if (currentStatus == IssueStatus.CLOSED) {
            throw new BusinessRuleException("Issue is already closed");
        }

        IssueResolution resolution = dto.getResolution();
        if (resolution == IssueResolution.FIXED && currentStatus != IssueStatus.IN_REVIEW) {
            throw new BusinessRuleException(
                    "FIXED resolution is only allowed when the issue is in IN_REVIEW status");
        }

        Release fixRelease = resolveRelease(projectId, dto.getFixReleaseId());

        issue.setStatus(IssueStatus.CLOSED);
        issue.setResolution(resolution);
        if (fixRelease != null) {
            issue.setFixRelease(fixRelease);
        }

        return issueMapper.toDto(issueRepository.save(issue));
    }

    @Transactional
    public IssueResponseDto reopenIssue(UUID projectId, UUID issueId, UUID userId) {
        Issue issue = findIssue(projectId, issueId);

        if (issue.getStatus() != IssueStatus.CLOSED) {
            throw new BusinessRuleException("Only CLOSED issues can be reopened");
        }

        ProjectMember member = findMember(projectId, userId);
        requireAnyRole(member,
                "Only a project member with TESTER or TEAM_LEAD role can reopen an issue",
                ProjectRole.TESTER, ProjectRole.TEAM_LEAD);

        issue.setStatus(IssueStatus.REOPENED);
        issue.setResolution(null);
        issue.setFixRelease(null);

        return issueMapper.toDto(issueRepository.save(issue));
    }

    private Project findActiveProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project with id " + projectId + " not found"));

        if (!project.isActive()) {
            throw new ResourceNotFoundException("Project with id " + projectId + " is not active");
        }

        return project;
    }

    private Issue findIssue(UUID projectId, UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issue with id " + issueId + " not found"));

        if (issue.getProject() == null || !issue.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException(
                    "Issue with id " + issueId + " not found in project " + projectId);
        }

        return issue;
    }

    private ProjectMember findMember(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " is not a member of project " + projectId));
    }

    private Component resolveComponent(UUID projectId, UUID componentId) {
        if (componentId == null) {
            return null;
        }

        return componentRepository.findById(componentId)
                .filter(component -> component.getProject() != null
                        && component.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Component with id " + componentId + " not found in project " + projectId));
    }

    private Release resolveRelease(UUID projectId, UUID releaseId) {
        if (releaseId == null) {
            return null;
        }

        return releaseRepository.findById(releaseId)
                .filter(release -> release.getProject() != null
                        && release.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Release with id " + releaseId + " not found in project " + projectId));
    }

    private Set<Tag> resolveTags(UUID projectId, Set<UUID> tagIds) {
        Set<Tag> tags = new HashSet<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return tags;
        }

        for (UUID tagId : tagIds) {
            if (tagId == null) {
                throw new ResourceNotFoundException("Tag not found in project " + projectId);
            }

            Tag tag = tagRepository.findById(tagId)
                    .filter(found -> found.getProject() != null
                            && found.getProject().getId().equals(projectId))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tag with id " + tagId + " not found in project " + projectId));

            tags.add(tag);
        }

        return tags;
    }

    private void requireAnyRole(ProjectMember member, String message, ProjectRole... roles) {
        for (ProjectRole role : roles) {
            if (member.getRole() == role) {
                return;
            }
        }

        throw new BusinessRuleException(message);
    }

    private boolean isAssignee(Issue issue, UUID userId) {
        return issue.getAssignee() != null && issue.getAssignee().getId().equals(userId);
    }

    private boolean isReporter(Issue issue, UUID userId) {
        return issue.getReporter() != null && issue.getReporter().getId().equals(userId);
    }
}
