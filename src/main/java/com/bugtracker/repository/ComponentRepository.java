package com.bugtracker.repository;

import com.bugtracker.domain.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {

    List<Component> findAllByProjectId(Long projectId);

    Optional<Component> findByProjectIdAndName(Long projectId, String name);

    boolean existsByProjectIdAndName(Long projectId, String name);
}
