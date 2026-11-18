package com.bugtracker.service;

import com.bugtracker.domain.entity.Issue;
import com.bugtracker.domain.entity.IssueLink;
import com.bugtracker.dto.IssueLinkCreateDto;
import com.bugtracker.dto.IssueLinkDto;
import com.bugtracker.exception.BusinessRuleException;
import com.bugtracker.exception.ConflictException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.IssueLinkMapper;
import com.bugtracker.repository.IssueLinkRepository;
import com.bugtracker.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueLinkService {

    private final IssueRepository issueRepository;
    private final IssueLinkRepository issueLinkRepository;
    private final IssueLinkMapper issueLinkMapper;

    @Transactional
    public IssueLinkDto addLink(UUID projectId, UUID sourceIssueId, IssueLinkCreateDto dto) {
        if (sourceIssueId.equals(dto.getTargetIssueId())) {
            throw new BusinessRuleException("An issue cannot be linked to itself");
        }

        Issue sourceIssue = findIssue(projectId, sourceIssueId);
        Issue targetIssue = findIssue(projectId, dto.getTargetIssueId());

        if (issueLinkRepository.existsBySourceIssueIdAndTargetIssueIdAndLinkType(
                sourceIssueId, dto.getTargetIssueId(), dto.getLinkType())) {
            throw new ConflictException(
                    "Link of type " + dto.getLinkType() + " between issues " + sourceIssueId
                            + " and " + dto.getTargetIssueId() + " already exists");
        }

        IssueLink link = IssueLink.builder()
                .sourceIssue(sourceIssue)
                .targetIssue(targetIssue)
                .linkType(dto.getLinkType())
                .build();

        return issueLinkMapper.toDto(issueLinkRepository.save(link));
    }

    public List<IssueLinkDto> getLinks(UUID projectId, UUID issueId) {
        findIssue(projectId, issueId);

        List<IssueLink> links = new ArrayList<>(issueLinkRepository.findAllBySourceIssueId(issueId));
        links.addAll(issueLinkRepository.findAllByTargetIssueId(issueId));

        return issueLinkMapper.toDtoList(links);
    }

    @Transactional
    public void removeLink(UUID projectId, UUID issueId, UUID linkId) {
        findIssue(projectId, issueId);

        IssueLink link = issueLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Link with id " + linkId + " not found"));

        boolean relatedToIssue = (link.getSourceIssue() != null
                && issueId.equals(link.getSourceIssue().getId()))
                || (link.getTargetIssue() != null
                && issueId.equals(link.getTargetIssue().getId()));

        if (!relatedToIssue) {
            throw new ResourceNotFoundException(
                    "Link with id " + linkId + " not found for issue " + issueId);
        }

        issueLinkRepository.delete(link);
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
}
