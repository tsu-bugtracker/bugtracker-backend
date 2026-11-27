package ru.tsu.bugtracker.release;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

	Page<Release> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

	Page<Release> findAllByProjectIdAndStatusOrderByCreatedAtDesc(
			UUID projectId, ReleaseStatus status, Pageable pageable);

	Optional<Release> findByIdAndProjectId(UUID id, UUID projectId);

	boolean existsByProjectIdAndVersionIgnoreCase(UUID projectId, String version);

	boolean existsByProjectIdAndVersionIgnoreCaseAndIdNot(UUID projectId, String version, UUID id);

}
