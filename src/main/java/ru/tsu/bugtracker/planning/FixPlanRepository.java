package ru.tsu.bugtracker.planning;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FixPlanRepository extends JpaRepository<FixPlan, UUID> {

	Optional<FixPlan> findByBugId(UUID bugId);

}
