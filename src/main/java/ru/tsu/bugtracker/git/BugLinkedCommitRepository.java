package ru.tsu.bugtracker.git;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BugLinkedCommitRepository extends JpaRepository<BugLinkedCommit, UUID> {

	List<BugLinkedCommit> findAllByBugIdOrderByLinkedAtDesc(UUID bugId);

	Optional<BugLinkedCommit> findByIdAndBugId(UUID id, UUID bugId);

	boolean existsByBugIdAndTypeAndCommitHash(UUID bugId, GitCommitType type, String commitHash);

	boolean existsByBugIdAndTypeAndMergeRequestUrl(UUID bugId, GitCommitType type, String mergeRequestUrl);

}
