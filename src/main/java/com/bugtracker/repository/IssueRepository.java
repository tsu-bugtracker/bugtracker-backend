package com.bugtracker.repository;

import com.bugtracker.domain.entity.Issue;
import com.bugtracker.domain.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    List<Issue> findAllByProjectId(UUID projectId);

    List<Issue> findAllByProjectIdAndStatus(UUID projectId, IssueStatus status);

    List<Issue> findAllByProjectIdAndAssigneeId(UUID projectId, UUID assigneeId);

    List<Issue> findAllByProjectIdAndReporterId(UUID projectId, UUID reporterId);
}
