package ru.tsu.bugtracker.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SoftwareComponentRepository extends JpaRepository<SoftwareComponent, UUID> {

	List<SoftwareComponent> findAllByProjectIdOrderByNameAsc(UUID projectId);

	Optional<SoftwareComponent> findByIdAndProjectId(UUID id, UUID projectId);

	boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

	boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

}
