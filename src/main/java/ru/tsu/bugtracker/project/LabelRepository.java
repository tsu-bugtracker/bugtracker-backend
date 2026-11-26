package ru.tsu.bugtracker.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, UUID> {

	List<Label> findAllByProjectIdOrderByNameAsc(UUID projectId);

	Optional<Label> findByIdAndProjectId(UUID id, UUID projectId);

	Optional<Label> findByProjectIdAndNameIgnoreCase(UUID projectId, String name);

	boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

	boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

}
