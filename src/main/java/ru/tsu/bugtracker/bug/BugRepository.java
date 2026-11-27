package ru.tsu.bugtracker.bug;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BugRepository extends JpaRepository<Bug, UUID>, JpaSpecificationExecutor<Bug> {

	long countByProjectIdAndStatusNot(UUID projectId, BugStatus status);

	List<Bug> findAllByProjectIdAndLabelsId(UUID projectId, UUID labelId);

	Optional<Bug> findByIdAndProjectId(UUID id, UUID projectId);

	boolean existsByProjectIdAndLabelsId(UUID projectId, UUID labelId);

	List<Bug> findAllByProjectIdAndStatusInOrderByCreatedAtDesc(UUID projectId, Collection<BugStatus> statuses);

	@Modifying
	@Query("update Bug b set b.component = null where b.component.id = :componentId")
	int clearComponent(@Param("componentId") UUID componentId);

	@Query("select count(b) from Bug b where b.targetRelease.id = :releaseId or b.fixedInRelease.id = :releaseId")
	long countByReleaseId(@Param("releaseId") UUID releaseId);

	@Query("select b from Bug b where b.targetRelease.id = :releaseId or b.fixedInRelease.id = :releaseId order by b.createdAt desc")
	List<Bug> findAllByReleaseId(@Param("releaseId") UUID releaseId);

	List<Bug> findAllByProjectIdAndIdIn(UUID projectId, Collection<UUID> ids);

	@Modifying
	@Query("update Bug b set b.targetRelease = null where b.targetRelease.id = :releaseId")
	int clearTargetRelease(@Param("releaseId") UUID releaseId);

	@Modifying
	@Query("update Bug b set b.fixedInRelease = null where b.fixedInRelease.id = :releaseId")
	int clearFixedInRelease(@Param("releaseId") UUID releaseId);

}
