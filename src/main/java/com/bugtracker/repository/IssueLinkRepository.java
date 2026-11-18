package com.bugtracker.repository;

import com.bugtracker.domain.entity.IssueLink;
import com.bugtracker.domain.enums.LinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueLinkRepository extends JpaRepository<IssueLink, UUID> {

    List<IssueLink> findAllBySourceIssueId(UUID sourceIssueId);

    List<IssueLink> findAllByTargetIssueId(UUID targetIssueId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndLinkType(UUID sourceIssueId, UUID targetIssueId, LinkType linkType);
}
