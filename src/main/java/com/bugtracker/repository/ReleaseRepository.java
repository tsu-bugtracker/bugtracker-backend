package com.bugtracker.repository;

import com.bugtracker.domain.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findAllByProjectId(Long projectId);

    Optional<Release> findByProjectIdAndVersionName(Long projectId, String versionName);

    boolean existsByProjectIdAndVersionName(Long projectId, String versionName);
}
