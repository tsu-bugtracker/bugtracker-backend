package com.bugtracker.repository;

import com.bugtracker.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByName(String name);

    boolean existsByName(String name);

    List<Project> findAllByCreatedById(UUID createdById);
}
