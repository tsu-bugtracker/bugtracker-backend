package ru.tsu.bugtracker.bug;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugHistoryRepository extends JpaRepository<BugHistory, UUID> {

	Page<BugHistory> findAllByBugIdOrderByCreatedAtDesc(UUID bugId, Pageable pageable);

}
