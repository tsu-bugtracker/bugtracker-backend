package ru.tsu.bugtracker.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

	List<ProjectMember> findAllByUserId(UUID userId);

	boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

	long countByProjectIdAndRoleAndStatus(UUID projectId, ProjectRole role, MemberStatus status);

	List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(UUID projectId);

	Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

	Optional<ProjectMember> findByIdAndProjectId(UUID id, UUID projectId);

	long countByProjectId(UUID projectId);

}
