package ru.tsu.bugtracker.planning;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.bug.BugStatus;
import ru.tsu.bugtracker.bug.BugSupportService;
import ru.tsu.bugtracker.bug.dto.BugDetailsDto;
import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.planning.dto.AssignBugRequest;
import ru.tsu.bugtracker.planning.dto.FixPlanRequest;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectMemberRepository;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.user.User;

/**
 * Planning workflow: fix plans, assignment of bugs and personal task lists.
 */
@Service
@Transactional
public class PlanningService {

	private static final String BUG_CLOSED_FOR_PLANNING = "Cannot plan a closed bug";

	private static final String BUG_CLOSED_FOR_ASSIGNMENT = "Cannot assign a closed bug";

	private static final String PRIORITY_REASON_REQUIRED =
			"priorityChangeReason is required when priority differs from recommendedPriority";

	private static final String ASSIGNEE_NOT_DEVELOPER =
			"Assignee must be an active developer in the project";

	private static final String INVALID_STATUS_FOR_TAKE =
			"Bug can only be taken in work from PLANNED or RETURNED status";

	private static final String ALREADY_ASSIGNED =
			"Bug is already assigned to another developer";

	private static final List<BugStatus> BACKLOG_STATUSES = List.of(
			BugStatus.NEW, BugStatus.PLANNED, BugStatus.REOPENED, BugStatus.RETURNED);

	private static final List<BugStatus> DEVELOPER_TASK_STATUSES = List.of(
			BugStatus.PLANNED, BugStatus.IN_PROGRESS, BugStatus.NEEDS_INFO, BugStatus.RETURNED);

	private static final List<BugStatus> TESTER_TASK_STATUSES = List.of(
			BugStatus.IN_REVIEW, BugStatus.NEEDS_INFO);

	private static final List<BugStatus> OPEN_STATUSES = List.of(
			BugStatus.NEW, BugStatus.PLANNED, BugStatus.IN_PROGRESS, BugStatus.NEEDS_INFO,
			BugStatus.IN_REVIEW, BugStatus.RETURNED, BugStatus.REOPENED);

	private final FixPlanRepository fixPlanRepository;

	private final BugRepository bugRepository;

	private final BugSupportService bugSupportService;

	private final ProjectAccessService projectAccessService;

	private final ProjectMemberRepository projectMemberRepository;

	public PlanningService(
			FixPlanRepository fixPlanRepository,
			BugRepository bugRepository,
			BugSupportService bugSupportService,
			ProjectAccessService projectAccessService,
			ProjectMemberRepository projectMemberRepository) {
		this.fixPlanRepository = fixPlanRepository;
		this.bugRepository = bugRepository;
		this.bugSupportService = bugSupportService;
		this.projectAccessService = projectAccessService;
		this.projectMemberRepository = projectMemberRepository;
	}

	/**
	 * Creates or updates a fix plan for a bug and applies the resulting priority, deadline and
	 * assignee to the bug itself.
	 *
	 * @param bugId  bug identifier
	 * @param userId user performing the planning
	 * @param req    planning payload
	 * @return updated bug details
	 */
	public BugDetailsDto planBugFix(UUID bugId, UUID userId, FixPlanRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (bug.getStatus() == BugStatus.CLOSED) {
			throw new ConflictException(BUG_CLOSED_FOR_PLANNING);
		}

		if (req.priority() != bug.getRecommendedPriority() && isBlank(req.priorityChangeReason())) {
			throw new BadRequestException(PRIORITY_REASON_REQUIRED);
		}

		User actor = this.bugSupportService.requireUser(userId);

		FixPlan plan = this.fixPlanRepository.findByBugId(bugId).orElseGet(FixPlan::new);
		plan.setBug(bug);
		plan.setPriority(req.priority());
		plan.setPriorityChangeReason(req.priorityChangeReason());
		plan.setDeadline(req.deadline());
		plan.setRationale(req.rationale().trim());
		plan.setPlannedBy(actor);
		this.fixPlanRepository.save(plan);

		BugPriority previousPriority = bug.getPriority();
		bug.setPriority(req.priority());
		if (previousPriority != req.priority()) {
			this.bugSupportService.recordHistory(bug, actor, "priority",
					previousPriority == null ? null : previousPriority.name(), req.priority().name());
		}

		Instant previousDeadline = bug.getDeadline();
		Instant newDeadline = req.deadline() != null ? req.deadline().toInstant() : null;
		bug.setDeadline(newDeadline);
		if (!Objects.equals(previousDeadline, newDeadline)) {
			this.bugSupportService.recordHistory(bug, actor, "deadline",
					previousDeadline == null ? null : previousDeadline.toString(),
					newDeadline == null ? null : newDeadline.toString());
		}

		if (req.assigneeId() != null) {
			ProjectMember assigneeMember = this.projectMemberRepository
				.findByProjectIdAndUserId(projectId, req.assigneeId())
				.filter((member) -> member.getRole() == ProjectRole.DEVELOPER)
				.orElseThrow(() -> new BadRequestException(ASSIGNEE_NOT_DEVELOPER));
			User assignee = assigneeMember.getUser();
			UUID previousAssigneeId = bug.getAssignee() == null ? null : bug.getAssignee().getId();
			bug.setAssignee(assignee);
			this.bugSupportService.recordHistory(bug, actor, "assignee",
					previousAssigneeId == null ? null : previousAssigneeId.toString(),
					assignee.getId().toString());
		}

		if (bug.getStatus() == BugStatus.NEW || bug.getStatus() == BugStatus.REOPENED) {
			BugStatus previousStatus = bug.getStatus();
			bug.setStatus(BugStatus.PLANNED);
			this.bugSupportService.recordHistory(bug, actor, "status",
					previousStatus.name(), BugStatus.PLANNED.name());
		}

		this.bugRepository.save(bug);
		return this.bugSupportService.toDetailsDto(bug);
	}

	/**
	 * Assigns a bug to a developer in the project.
	 *
	 * @param bugId  bug identifier
	 * @param userId user performing the assignment
	 * @param req    assignment payload
	 * @return updated bug details
	 */
	public BugDetailsDto assignBug(UUID bugId, UUID userId, AssignBugRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (bug.getStatus() == BugStatus.CLOSED) {
			throw new ConflictException(BUG_CLOSED_FOR_ASSIGNMENT);
		}

		ProjectMember assigneeMember = this.projectMemberRepository
			.findByProjectIdAndUserId(projectId, req.assigneeId())
			.filter((member) -> member.getRole() == ProjectRole.DEVELOPER)
			.orElseThrow(() -> new BadRequestException(ASSIGNEE_NOT_DEVELOPER));

		User actor = this.bugSupportService.requireUser(userId);
		User assignee = assigneeMember.getUser();
		UUID previousAssigneeId = bug.getAssignee() == null ? null : bug.getAssignee().getId();
		bug.setAssignee(assignee);
		this.bugSupportService.recordHistory(bug, actor, "assignee",
				previousAssigneeId == null ? null : previousAssigneeId.toString(),
				assignee.getId().toString());

		this.bugRepository.save(bug);
		return this.bugSupportService.toDetailsDto(bug);
	}

	/**
	 * Takes a planned or returned bug in work by the current developer.
	 *
	 * @param bugId  bug identifier
	 * @param userId developer taking the bug
	 * @return updated bug details
	 */
	public BugDetailsDto takeBugInWork(UUID bugId, UUID userId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.DEVELOPER);

		if (bug.getStatus() != BugStatus.PLANNED && bug.getStatus() != BugStatus.RETURNED) {
			throw new ConflictException(INVALID_STATUS_FOR_TAKE);
		}

		if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(userId)) {
			throw new ConflictException(ALREADY_ASSIGNED);
		}

		User actor = this.bugSupportService.requireUser(userId);
		if (bug.getAssignee() == null) {
			bug.setAssignee(actor);
			this.bugSupportService.recordHistory(bug, actor, "assignee",
					null, actor.getId().toString());
		}

		BugStatus previousStatus = bug.getStatus();
		bug.setStatus(BugStatus.IN_PROGRESS);
		this.bugSupportService.recordHistory(bug, actor, "status",
				previousStatus.name(), BugStatus.IN_PROGRESS.name());

		this.bugRepository.save(bug);
		return this.bugSupportService.toDetailsDto(bug);
	}

	/**
	 * Returns a page of the planning backlog of a project.
	 *
	 * @param projectId   project identifier
	 * @param userId      user requesting the backlog
	 * @param priorities  optional priority filter
	 * @param componentId optional component filter
	 * @param pageable    pagination and sorting
	 * @return page of bugs waiting for planning or rework
	 */
	@Transactional(readOnly = true)
	public PagedResponse<BugSummaryDto> getProjectBacklog(
			UUID projectId,
			UUID userId,
			List<BugPriority> priorities,
			UUID componentId,
			Pageable pageable) {
		this.projectAccessService.requireRole(
				projectId, userId, ProjectRole.TEAM_LEAD, ProjectRole.DEVELOPER);

		Specification<Bug> specification = (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));
			predicates.add(root.get("status").in(BACKLOG_STATUSES));
			if (priorities != null && !priorities.isEmpty()) {
				predicates.add(root.get("priority").in(priorities));
			}
			if (componentId != null) {
				predicates.add(criteriaBuilder.equal(root.get("component").get("id"), componentId));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};

		Pageable effectivePageable = pageable;
		if (pageable.getSort().isUnsorted()) {
			effectivePageable = PageRequest.of(
					pageable.getPageNumber(), pageable.getPageSize(),
					Sort.by(Sort.Direction.DESC, "createdAt"));
		}

		Page<BugSummaryDto> page = this.bugRepository.findAll(specification, effectivePageable)
			.map(this.bugSupportService::toSummaryDto);
		return PagedResponse.from(page);
	}

	/**
	 * Returns tasks relevant to the given user, optionally limited to a single project and
	 * filtered by status.
	 *
	 * @param userId    user whose tasks are requested
	 * @param projectId optional project filter
	 * @param statuses  optional status filter
	 * @param pageable  pagination
	 * @return page of deduplicated tasks ordered by creation date, newest first
	 */
	@Transactional(readOnly = true)
	public PagedResponse<BugSummaryDto> getMyTasks(
			UUID userId,
			UUID projectId,
			List<BugStatus> statuses,
			Pageable pageable) {
		Map<UUID, Bug> collected = new LinkedHashMap<>();

		if (projectId != null) {
			ProjectMember membership = this.projectAccessService.requireMember(projectId, userId);
			for (Bug bug : collectTasks(projectId, userId, membership.getRole())) {
				collected.putIfAbsent(bug.getId(), bug);
			}
		} else {
			for (ProjectMember membership : this.projectMemberRepository.findAllByUserId(userId)) {
				UUID memberProjectId = membership.getProject().getId();
				for (Bug bug : collectTasks(memberProjectId, userId, membership.getRole())) {
					collected.putIfAbsent(bug.getId(), bug);
				}
			}
		}

		List<Bug> allBugs = new ArrayList<>(collected.values());
		if (statuses != null && !statuses.isEmpty()) {
			allBugs.removeIf((bug) -> !statuses.contains(bug.getStatus()));
		}

		allBugs.sort(Comparator.comparing(Bug::getCreatedAt,
				Comparator.nullsLast(Comparator.reverseOrder())));

		int fromIndex = (int) Math.min(pageable.getOffset(), allBugs.size());
		int toIndex = Math.min(fromIndex + pageable.getPageSize(), allBugs.size());
		List<BugSummaryDto> items = allBugs.subList(fromIndex, toIndex)
			.stream()
			.map(this.bugSupportService::toSummaryDto)
			.toList();
		return PagedResponse.from(new PageImpl<>(items, pageable, allBugs.size()));
	}

	private List<Bug> collectTasks(UUID projectId, UUID userId, ProjectRole role) {
		if (role == ProjectRole.DEVELOPER) {
			return this.bugRepository
				.findAllByProjectIdAndStatusInOrderByCreatedAtDesc(projectId, DEVELOPER_TASK_STATUSES)
				.stream()
				.filter((bug) -> isOwnedBy(bug, userId)
						|| (bug.getStatus() == BugStatus.PLANNED && bug.getAssignee() == null))
				.toList();
		}

		if (role == ProjectRole.TESTER) {
			return this.bugRepository
				.findAllByProjectIdAndStatusInOrderByCreatedAtDesc(projectId, TESTER_TASK_STATUSES);
		}

		if (role == ProjectRole.TEAM_LEAD) {
			return this.bugRepository
				.findAllByProjectIdAndStatusInOrderByCreatedAtDesc(projectId, OPEN_STATUSES)
				.stream()
				.filter((bug) -> bug.getStatus() == BugStatus.NEW
						|| bug.getStatus() == BugStatus.REOPENED
						|| this.bugSupportService.isOverdue(bug))
				.toList();
		}

		return List.of();
	}

	private static boolean isOwnedBy(Bug bug, UUID userId) {
		return bug.getAssignee() != null && bug.getAssignee().getId().equals(userId);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
