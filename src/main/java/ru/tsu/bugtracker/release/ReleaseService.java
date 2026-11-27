package ru.tsu.bugtracker.release;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.bug.BugSupportService;
import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.planning.FixPlanRepository;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.release.dto.CreateReleaseRequest;
import ru.tsu.bugtracker.release.dto.PublishReleaseRequest;
import ru.tsu.bugtracker.release.dto.ReleaseDetailsDto;
import ru.tsu.bugtracker.release.dto.ReleaseSummaryDto;
import ru.tsu.bugtracker.release.dto.SetReleaseBugsRequest;
import ru.tsu.bugtracker.release.dto.UpdateReleaseRequest;
import ru.tsu.bugtracker.user.User;

/**
 * Application service for managing project releases and their bug scope.
 */
@Service
@Transactional
public class ReleaseService {

	private static final String RELEASE_NOT_FOUND = "Release not found";

	private static final String VERSION_REQUIRED = "Release version is required";

	private static final String VERSION_ALREADY_EXISTS = "Release version already exists";

	private static final String SOME_BUGS_NOT_IN_PROJECT = "Some bugs do not belong to this project";

	private static final String ONLY_PLANNED_CAN_BE_UPDATED = "Only PLANNED release can be updated";

	private static final String ONLY_PLANNED_CAN_BE_DELETED = "Only PLANNED release can be deleted";

	private static final String BUGS_ONLY_FOR_PLANNED = "Bugs can only be changed for PLANNED release";

	private static final String RELEASE_ALREADY_PUBLISHED = "Release is already published";

	private final ReleaseRepository releaseRepository;

	private final BugRepository bugRepository;

	private final FixPlanRepository fixPlanRepository;

	private final ProjectAccessService projectAccessService;

	private final BugSupportService bugSupportService;

	public ReleaseService(
			ReleaseRepository releaseRepository,
			BugRepository bugRepository,
			FixPlanRepository fixPlanRepository,
			ProjectAccessService projectAccessService,
			BugSupportService bugSupportService) {
		this.releaseRepository = releaseRepository;
		this.bugRepository = bugRepository;
		this.fixPlanRepository = fixPlanRepository;
		this.projectAccessService = projectAccessService;
		this.bugSupportService = bugSupportService;
	}

	/**
	 * Returns a page of releases belonging to the given project.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @param status    optional status filter
	 * @param page      zero-based page index
	 * @param size      requested page size
	 * @return page of release summaries
	 */
	@Transactional(readOnly = true)
	public PagedResponse<ReleaseSummaryDto> listProjectReleases(
			UUID projectId, UUID userId, ReleaseStatus status, int page, int size) {
		this.projectAccessService.requireMember(projectId, userId);
		Pageable pageable = PageRequest.of(
				Math.max(page, 0), Math.clamp(size, 1, 100));
		Page<Release> releases = status != null
				? this.releaseRepository.findAllByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status, pageable)
				: this.releaseRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId, pageable);
		return PagedResponse.from(releases.map(this::toSummaryDto));
	}

	/**
	 * Returns the details of a release visible to the given user.
	 *
	 * @param releaseId release identifier
	 * @param userId    identifier of the acting user
	 * @return release details
	 */
	@Transactional(readOnly = true)
	public ReleaseDetailsDto getRelease(UUID releaseId, UUID userId) {
		Release release = requireRelease(releaseId);
		this.projectAccessService.requireMember(release.getProject().getId(), userId);
		return toDetailsDto(release);
	}

	/**
	 * Creates a planned release in the given project.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @param req       creation payload
	 * @return created release details
	 */
	public ReleaseDetailsDto createRelease(UUID projectId, UUID userId, CreateReleaseRequest req) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD).getUser();

		String version = req.version() == null ? "" : req.version().trim();
		if (version.isEmpty()) {
			throw new BadRequestException(VERSION_REQUIRED);
		}
		if (this.releaseRepository.existsByProjectIdAndVersionIgnoreCase(projectId, version)) {
			throw new ConflictException(VERSION_ALREADY_EXISTS);
		}

		Release release = new Release();
		release.setProject(project);
		release.setVersion(version);
		release.setName(trimToNull(req.name()));
		release.setDescription(trimToNull(req.description()));
		release.setPlannedDate(req.plannedDate());
		release.setStatus(ReleaseStatus.PLANNED);
		release.setCreatedBy(actor);
		Release saved = this.releaseRepository.save(release);

		if (req.bugIds() != null && !req.bugIds().isEmpty()) {
			Set<UUID> uniqueIds = uniqueIds(req.bugIds());
			if (!uniqueIds.isEmpty()) {
				List<Bug> bugs = this.bugRepository.findAllByProjectIdAndIdIn(projectId, uniqueIds);
				if (bugs.size() != uniqueIds.size()) {
					throw new BadRequestException(SOME_BUGS_NOT_IN_PROJECT);
				}
				for (Bug bug : bugs) {
					bug.setTargetRelease(saved);
				}
				this.bugRepository.saveAll(bugs);
			}
		}

		return toDetailsDto(saved);
	}

	/**
	 * Updates the editable fields of a planned release.
	 *
	 * @param releaseId release identifier
	 * @param userId    identifier of the acting user
	 * @param req       update payload; {@code null} fields are left unchanged
	 * @return updated release details
	 */
	public ReleaseDetailsDto updateRelease(UUID releaseId, UUID userId, UpdateReleaseRequest req) {
		Release release = requireRelease(releaseId);
		UUID projectId = release.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (release.getStatus() != ReleaseStatus.PLANNED) {
			throw new ConflictException(ONLY_PLANNED_CAN_BE_UPDATED);
		}

		if (req.version() != null) {
			String version = req.version().trim();
			if (version.isEmpty()) {
				throw new BadRequestException(VERSION_REQUIRED);
			}
			if (this.releaseRepository.existsByProjectIdAndVersionIgnoreCaseAndIdNot(
					projectId, version, releaseId)) {
				throw new ConflictException(VERSION_ALREADY_EXISTS);
			}
			release.setVersion(version);
		}
		if (req.name() != null) {
			release.setName(trimToNull(req.name()));
		}
		if (req.description() != null) {
			release.setDescription(trimToNull(req.description()));
		}
		if (req.plannedDate() != null) {
			release.setPlannedDate(req.plannedDate());
		}

		Release saved = this.releaseRepository.save(release);
		return toDetailsDto(saved);
	}

	/**
	 * Deletes a planned release and detaches it from all related bugs and fix plans.
	 *
	 * @param releaseId release identifier
	 * @param userId    identifier of the acting user
	 */
	public void deleteRelease(UUID releaseId, UUID userId) {
		Release release = requireRelease(releaseId);
		UUID projectId = release.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (release.getStatus() != ReleaseStatus.PLANNED) {
			throw new ConflictException(ONLY_PLANNED_CAN_BE_DELETED);
		}

		this.bugRepository.clearTargetRelease(releaseId);
		this.bugRepository.clearFixedInRelease(releaseId);
		this.fixPlanRepository.clearTargetRelease(releaseId);
		this.releaseRepository.delete(release);
	}

	/**
	 * Replaces the set of bugs targeted at a planned release.
	 *
	 * @param releaseId release identifier
	 * @param userId    identifier of the acting user
	 * @param req       payload with the full desired set of bug identifiers
	 * @return updated release details
	 */
	public ReleaseDetailsDto setReleaseBugs(UUID releaseId, UUID userId, SetReleaseBugsRequest req) {
		Release release = requireRelease(releaseId);
		UUID projectId = release.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (release.getStatus() != ReleaseStatus.PLANNED) {
			throw new ConflictException(BUGS_ONLY_FOR_PLANNED);
		}

		List<Bug> current = this.bugRepository.findAllByReleaseId(releaseId);
		List<Bug> detached = new ArrayList<>();
		for (Bug bug : current) {
			if (bug.getTargetRelease() != null && releaseId.equals(bug.getTargetRelease().getId())) {
				bug.setTargetRelease(null);
				detached.add(bug);
			}
		}
		if (!detached.isEmpty()) {
			this.bugRepository.saveAll(detached);
		}

		if (!req.bugIds().isEmpty()) {
			Set<UUID> uniqueIds = uniqueIds(req.bugIds());
			if (!uniqueIds.isEmpty()) {
				List<Bug> bugs = this.bugRepository.findAllByProjectIdAndIdIn(projectId, uniqueIds);
				if (bugs.size() != uniqueIds.size()) {
					throw new BadRequestException(SOME_BUGS_NOT_IN_PROJECT);
				}
				for (Bug bug : bugs) {
					bug.setTargetRelease(release);
				}
				this.bugRepository.saveAll(bugs);
			}
		}

		return toDetailsDto(release);
	}

	/**
	 * Publishes a planned release.
	 *
	 * @param releaseId release identifier
	 * @param userId    identifier of the acting user
	 * @param req       optional publish payload with release notes
	 * @return updated release details
	 */
	public ReleaseDetailsDto publishRelease(UUID releaseId, UUID userId, PublishReleaseRequest req) {
		Release release = requireRelease(releaseId);
		UUID projectId = release.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (release.getStatus() != ReleaseStatus.PLANNED) {
			throw new ConflictException(RELEASE_ALREADY_PUBLISHED);
		}

		release.setStatus(ReleaseStatus.RELEASED);
		release.setReleasedAt(Instant.now());
		if (req != null && req.releaseNotes() != null) {
			release.setReleaseNotes(trimToNull(req.releaseNotes()));
		}

		Release saved = this.releaseRepository.save(release);
		return toDetailsDto(saved);
	}

	private Release requireRelease(UUID releaseId) {
		return this.releaseRepository.findById(releaseId)
			.orElseThrow(() -> new NotFoundException(RELEASE_NOT_FOUND));
	}

	private ReleaseSummaryDto toSummaryDto(Release release) {
		return new ReleaseSummaryDto(
				release.getId(),
				release.getProject().getId(),
				release.getVersion(),
				release.getName(),
				release.getStatus(),
				release.getPlannedDate(),
				release.getReleasedAt(),
				this.bugRepository.countByReleaseId(release.getId()),
				release.getCreatedAt());
	}

	private ReleaseDetailsDto toDetailsDto(Release release) {
		List<BugSummaryDto> bugs = this.bugRepository.findAllByReleaseId(release.getId())
			.stream()
			.map(this.bugSupportService::toSummaryDto)
			.toList();
		return new ReleaseDetailsDto(
				release.getId(),
				release.getProject().getId(),
				release.getVersion(),
				release.getName(),
				release.getDescription(),
				release.getReleaseNotes(),
				release.getStatus(),
				release.getPlannedDate(),
				release.getReleasedAt(),
				bugs.size(),
				bugs,
				release.getCreatedAt(),
				release.getUpdatedAt());
	}

	private Set<UUID> uniqueIds(List<UUID> ids) {
		return ids.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
