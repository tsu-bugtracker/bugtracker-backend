package com.bugtracker.repository;

import com.bugtracker.domain.entity.ProjectMember;
import com.bugtracker.domain.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findAllByProjectId(UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    long countByProjectIdAndRole(UUID projectId, ProjectRole role);

    boolean existsByProjectIdAndRole(UUID projectId, ProjectRole role);
}
