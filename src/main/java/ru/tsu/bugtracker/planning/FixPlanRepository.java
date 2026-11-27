package ru.tsu.bugtracker.planning;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixPlanRepository extends JpaRepository<FixPlan, UUID> {

	Optional<FixPlan> findByBugId(UUID bugId);

	@Modifying
	@Query("update FixPlan f set f.targetRelease = null where f.targetRelease.id = :releaseId")
	int clearTargetRelease(@Param("releaseId") UUID releaseId);

}
