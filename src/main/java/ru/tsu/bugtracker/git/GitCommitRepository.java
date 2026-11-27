package ru.tsu.bugtracker.git;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GitCommitRepository
		extends JpaRepository<GitCommit, UUID>, JpaSpecificationExecutor<GitCommit> {

	Optional<GitCommit> findByProjectIdAndTypeAndCommitHash(UUID projectId, GitCommitType type, String commitHash);

	Optional<GitCommit> findByProjectIdAndTypeAndMergeRequestUrl(
			UUID projectId, GitCommitType type, String mergeRequestUrl);

}
