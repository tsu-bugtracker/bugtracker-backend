package ru.tsu.bugtracker.bug;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.dto.BugDetailsDto;
import ru.tsu.bugtracker.bug.dto.BugHistoryEntryDto;
import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.bug.dto.CreateBugRequest;
import ru.tsu.bugtracker.bug.dto.UpdateBugRequest;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.project.Label;
import ru.tsu.bugtracker.project.LabelRepository;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.project.SoftwareComponent;
import ru.tsu.bugtracker.project.SoftwareComponentRepository;
import ru.tsu.bugtracker.user.User;

/**
 * Application service for creating, reading and updating bugs.
 */
@Service
@Transactional
public class BugService {

	private static final String BUG_TRACKING_DISABLED =
			"Bug tracking is disabled until an active tester is assigned";

	private static final String COMPONENT_NOT_FOUND = "Component not found";

	private static final String LABEL_NOT_FOUND = "Label not found";

	private final BugSupportService bugSupportService;

	private final BugRepository bugRepository;

	private final BugHistoryRepository bugHistoryRepository;

	private final ProjectAccessService projectAccessService;

	private final SoftwareComponentRepository softwareComponentRepository;

	private final LabelRepository labelRepository;

	public BugService(
			BugSupportService bugSupportService,
			BugRepository bugRepository,
			BugHistoryRepository bugHistoryRepository,
			ProjectAccessService projectAccessService,
			SoftwareComponentRepository softwareComponentRepository,
			LabelRepository labelRepository) {
		this.bugSupportService = bugSupportService;
		this.bugRepository = bugRepository;
		this.bugHistoryRepository = bugHistoryRepository;
		this.projectAccessService = projectAccessService;
		this.softwareComponentRepository = softwareComponentRepository;
		this.labelRepository = labelRepository;
	}

	/**
	 * Creates a bug in the given project.
	 *
	 * @param projectId project identifier
	 * @param userId    identifier of the acting user
	 * @param req       creation payload
	 * @return created bug
	 */
	public BugDetailsDto createBug(UUID projectId, UUID userId, CreateBugRequest req) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TESTER);
		if (!project.isBugTrackingEnabled()) {
			throw new ConflictException(BUG_TRACKING_DISABLED);
		}

		long sequence = project.getBugSequence() + 1;
		project.setBugSequence(sequence);
		String bugKey = "BUG-" + sequence;

		SoftwareComponent component = resolveComponent(projectId, req.componentId());
		Set<Label> labels = resolveLabels(project, req.labelIds(), req.newLabels());
		User author = this.bugSupportService.requireUser(userId);

		Bug bug = new Bug();
		bug.setProject(project);
		bug.setBugKey(bugKey);
		bug.setTitle(req.title());
		bug.setDescription(req.description());
		bug.setStepsToReproduce(req.stepsToReproduce());
		bug.setEnvironment(req.environment());
		bug.setExpectedResult(req.expectedResult());
		bug.setActualResult(req.actualResult());
		bug.setStatus(BugStatus.NEW);
		bug.setPriority(req.recommendedPriority());
		bug.setRecommendedPriority(req.recommendedPriority());
		bug.setAuthor(author);
		bug.setComponent(component);
		bug.setLabels(new HashSet<>(labels));

		Bug saved = this.bugRepository.save(bug);
		this.bugSupportService.recordHistory(saved, author, "status", null, BugStatus.NEW.name());
		return this.bugSupportService.toDetailsDto(saved);
	}

	/**
	 * Returns a filtered page of bugs belonging to the given project.
	 *
	 * @param projectId   project identifier
	 * @param userId      identifier of the acting user
	 * @param status      optional status filter
	 * @param priority    optional priority filter
	 * @param assigneeId  optional assignee filter
	 * @param componentId optional component filter
	 * @param labelId     optional label filter
	 * @param search      optional case-insensitive title or key search
	 * @param overdue     optional overdue filter
	 * @param pageable    pagination and sorting
	 * @return page of bug summaries
	 */
	@Transactional(readOnly = true)
	public PagedResponse<BugSummaryDto> listProjectBugs(
			UUID projectId,
			UUID userId,
			BugStatus status,
			BugPriority priority,
			UUID assigneeId,
			UUID componentId,
			UUID labelId,
			String search,
			Boolean overdue,
			Pageable pageable) {
		this.projectAccessService.requireMember(projectId, userId);
		Specification<Bug> specification = buildSpecification(
				projectId, status, priority, assigneeId, componentId, labelId, search, overdue);
		Page<BugSummaryDto> page = this.bugRepository.findAll(specification, pageable)
			.map(this.bugSupportService::toSummaryDto);
		return PagedResponse.from(page);
	}

	/**
	 * Returns the details of a bug visible to the given user.
	 *
	 * @param bugId  bug identifier
	 * @param userId identifier of the acting user
	 * @return bug details
	 */
	@Transactional(readOnly = true)
	public BugDetailsDto getBug(UUID bugId, UUID userId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		this.projectAccessService.requireMember(bug.getProject().getId(), userId);
		return this.bugSupportService.toDetailsDto(bug);
	}

	/**
	 * Updates the editable fields of a bug and records history entries for the changes.
	 *
	 * @param bugId  bug identifier
	 * @param userId identifier of the acting user
	 * @param req    update payload; {@code null} fields are left unchanged
	 * @return updated bug details
	 */
	public BugDetailsDto updateBug(UUID bugId, UUID userId, UpdateBugRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		Project project = this.projectAccessService.requireActiveProject(bug.getProject().getId());
		this.projectAccessService.requireRole(
				project.getId(), userId, ProjectRole.TESTER, ProjectRole.DEVELOPER, ProjectRole.TEAM_LEAD);
		User actor = this.bugSupportService.requireUser(userId);

		updateText(bug, actor, "title", bug.getTitle(), req.title(), bug::setTitle);
		updateText(bug, actor, "description", bug.getDescription(), req.description(), bug::setDescription);
		updateText(bug, actor, "stepsToReproduce", bug.getStepsToReproduce(), req.stepsToReproduce(),
				bug::setStepsToReproduce);
		updateText(bug, actor, "environment", bug.getEnvironment(), req.environment(), bug::setEnvironment);
		updateText(bug, actor, "expectedResult", bug.getExpectedResult(), req.expectedResult(),
				bug::setExpectedResult);
		updateText(bug, actor, "actualResult", bug.getActualResult(), req.actualResult(), bug::setActualResult);

		if (req.componentId() != null) {
			SoftwareComponent component = resolveComponent(project.getId(), req.componentId());
			UUID currentId = bug.getComponent() == null ? null : bug.getComponent().getId();
			if (!Objects.equals(currentId, component.getId())) {
				String oldValue = bug.getComponent() == null ? null : bug.getComponent().getName();
				bug.setComponent(component);
				this.bugSupportService.recordHistory(bug, actor, "component", oldValue, component.getName());
			}
		}

		if (req.labelIds() != null || req.newLabels() != null) {
			Set<Label> labels = resolveLabels(project, req.labelIds(), req.newLabels());
			String oldValue = formatLabels(bug.getLabels());
			String newValue = formatLabels(labels);
			if (!Objects.equals(oldValue, newValue)) {
				bug.setLabels(new HashSet<>(labels));
				this.bugSupportService.recordHistory(bug, actor, "labels", oldValue, newValue);
			}
		}

		Bug saved = this.bugRepository.save(bug);
		return this.bugSupportService.toDetailsDto(saved);
	}

	/**
	 * Returns a page of history entries for the given bug, newest first.
	 *
	 * @param bugId    bug identifier
	 * @param userId   identifier of the acting user
	 * @param pageable pagination
	 * @return page of history entries
	 */
	@Transactional(readOnly = true)
	public PagedResponse<BugHistoryEntryDto> getBugHistory(UUID bugId, UUID userId, Pageable pageable) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		this.projectAccessService.requireMember(bug.getProject().getId(), userId);
		Page<BugHistoryEntryDto> page = this.bugHistoryRepository
			.findAllByBugIdOrderByCreatedAtDesc(bugId, pageable)
			.map(BugHistoryEntryDto::from);
		return PagedResponse.from(page);
	}

	private Specification<Bug> buildSpecification(
			UUID projectId,
			BugStatus status,
			BugPriority priority,
			UUID assigneeId,
			UUID componentId,
			UUID labelId,
			String search,
			Boolean overdue) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("project").get("id"), projectId));

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (priority != null) {
				predicates.add(cb.equal(root.get("priority"), priority));
			}
			if (assigneeId != null) {
				predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
			}
			if (componentId != null) {
				predicates.add(cb.equal(root.get("component").get("id"), componentId));
			}
			if (labelId != null) {
				query.distinct(true);
				predicates.add(cb.equal(root.join("labels").get("id"), labelId));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.<String>get("title")), pattern),
						cb.like(cb.lower(root.<String>get("bugKey")), pattern)));
			}
			if (overdue != null) {
				Instant now = Instant.now();
				if (overdue) {
					predicates.add(cb.and(
							cb.isNotNull(root.get("deadline")),
							cb.lessThan(root.<Instant>get("deadline"), now),
							cb.notEqual(root.get("status"), BugStatus.CLOSED)));
				}
				else {
					predicates.add(cb.or(
							cb.isNull(root.get("deadline")),
							cb.greaterThanOrEqualTo(root.<Instant>get("deadline"), now),
							cb.equal(root.get("status"), BugStatus.CLOSED)));
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private SoftwareComponent resolveComponent(UUID projectId, UUID componentId) {
		if (componentId == null) {
			return null;
		}
		return this.softwareComponentRepository.findByIdAndProjectId(componentId, projectId)
			.orElseThrow(() -> new NotFoundException(COMPONENT_NOT_FOUND));
	}

	private Set<Label> resolveLabels(Project project, List<UUID> labelIds, List<String> newLabels) {
		Map<UUID, Label> resolved = new LinkedHashMap<>();
		if (labelIds != null) {
			for (UUID labelId : labelIds) {
				if (labelId == null) {
					continue;
				}
				Label label = this.labelRepository.findByIdAndProjectId(labelId, project.getId())
					.orElseThrow(() -> new NotFoundException(LABEL_NOT_FOUND));
				resolved.put(label.getId(), label);
			}
		}
		if (newLabels != null) {
			Set<String> seenNames = new HashSet<>();
			for (String rawName : newLabels) {
				if (rawName == null || rawName.isBlank()) {
					continue;
				}
				String name = rawName.trim();
				if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
					continue;
				}
				Label label = this.labelRepository.findByProjectIdAndNameIgnoreCase(project.getId(), name)
					.orElseGet(() -> {
						Label created = new Label();
						created.setProject(project);
						created.setName(name);
						return this.labelRepository.save(created);
					});
				resolved.put(label.getId(), label);
			}
		}
		return new LinkedHashSet<>(resolved.values());
	}

	private void updateText(
			Bug bug,
			User actor,
			String fieldName,
			String currentValue,
			String newValue,
			Consumer<String> setter) {
		if (newValue != null && !Objects.equals(currentValue, newValue)) {
			setter.accept(newValue);
			this.bugSupportService.recordHistory(bug, actor, fieldName, currentValue, newValue);
		}
	}

	private String formatLabels(Set<Label> labels) {
		if (labels == null || labels.isEmpty()) {
			return null;
		}
		return labels.stream()
			.map(Label::getName)
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.collect(Collectors.joining(", "));
	}

}
