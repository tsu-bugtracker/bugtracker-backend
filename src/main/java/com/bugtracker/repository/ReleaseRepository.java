package com.bugtracker.repository;

import com.bugtracker.domain.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    List<Release> findAllByProjectId(UUID projectId);

    Optional<Release> findByProjectIdAndVersionName(UUID projectId, String versionName);

    boolean existsByProjectIdAndVersionName(UUID projectId, String versionName);
}
