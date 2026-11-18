package com.bugtracker.repository;

import com.bugtracker.domain.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<Component, UUID> {

    List<Component> findAllByProjectId(UUID projectId);

    Optional<Component> findByProjectIdAndName(UUID projectId, String name);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}
