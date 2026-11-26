package ru.tsu.bugtracker.bug;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BugLinkRepository extends JpaRepository<BugLink, UUID> {

	List<BugLink> findAllBySourceBugIdOrderByCreatedAtAsc(UUID sourceBugId);

	Optional<BugLink> findByIdAndSourceBugId(UUID id, UUID sourceBugId);

	boolean existsBySourceBugIdAndTargetBugIdAndLinkType(UUID sourceBugId, UUID targetBugId, BugLinkType linkType);

}
