package com.bugtracker.repository;

import com.bugtracker.domain.entity.Issue;
import com.bugtracker.domain.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findAllByProjectId(Long projectId);

    List<Issue> findAllByProjectIdAndStatus(Long projectId, IssueStatus status);

    List<Issue> findAllByProjectIdAndAssigneeId(Long projectId, Long assigneeId);

    List<Issue> findAllByProjectIdAndReporterId(Long projectId, Long reporterId);
}
