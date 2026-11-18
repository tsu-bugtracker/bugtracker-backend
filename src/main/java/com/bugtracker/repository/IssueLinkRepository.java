package com.bugtracker.repository;

import com.bugtracker.domain.entity.IssueLink;
import com.bugtracker.domain.enums.LinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueLinkRepository extends JpaRepository<IssueLink, Long> {

    List<IssueLink> findAllBySourceIssueId(Long sourceIssueId);

    List<IssueLink> findAllByTargetIssueId(Long targetIssueId);

    boolean existsBySourceIssueIdAndTargetIssueIdAndLinkType(Long sourceIssueId, Long targetIssueId, LinkType linkType);
}
