package ru.tsu.bugtracker.bug;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.dto.BugDetailsDto;
import ru.tsu.bugtracker.bug.dto.ClarifyBugRequest;
import ru.tsu.bugtracker.bug.dto.CloseWithoutFixRequest;
import ru.tsu.bugtracker.bug.dto.FixBugRequest;
import ru.tsu.bugtracker.bug.dto.ReopenBugRequest;
import ru.tsu.bugtracker.bug.dto.RequestClarificationRequest;
import ru.tsu.bugtracker.bug.dto.VerifyBugRequest;
import ru.tsu.bugtracker.bug.dto.VerifyDecision;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.user.User;

/**
 * Implements the bug lifecycle transitions: requesting and supplying clarification, submitting a
 * fix, verifying the fix, closing without a fix and reopening a closed bug.
 */
@Service
@Transactional
public class BugLifecycleService {

	private static final String STATUS_FIELD = "status";

	private static final String RESOLUTION_FIELD = "resolution";

	private static final String STEPS_TO_REPRODUCE_FIELD = "stepsToReproduce";

	private static final String ENVIRONMENT_FIELD = "environment";

	private final BugSupportService bugSupportService;

	private final BugRepository bugRepository;

	private final ProjectAccessService projectAccessService;

	public BugLifecycleService(
			BugSupportService bugSupportService,
			BugRepository bugRepository,
			ProjectAccessService projectAccessService) {
		this.bugSupportService = bugSupportService;
		this.bugRepository = bugRepository;
		this.projectAccessService = projectAccessService;
	}

	/**
	 * Moves a bug from {@code IN_PROGRESS} to {@code NEEDS_INFO} and records the developer's
	 * question as a system comment.
	 *
	 * @param bugId bug identifier
	 * @param userId acting user identifier
	 * @param req    clarification request
	 * @return updated bug details
	 */
	public BugDetailsDto requestClarification(UUID bugId, UUID userId, RequestClarificationRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.DEVELOPER)
			.getUser();

		if (bug.getStatus() != BugStatus.IN_PROGRESS) {
			throw new ConflictException("Clarification can only be requested for a bug in IN_PROGRESS status");
		}

		bug.setStatus(BugStatus.NEEDS_INFO);
		this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
				BugStatus.IN_PROGRESS.name(), BugStatus.NEEDS_INFO.name());
		this.bugSupportService.addSystemComment(bug, actor, req.question());

		return saveAndMap(bug);
	}

	/**
	 * Supplies the requested information, optionally updating reproduction details, and returns the
	 * bug to {@code IN_PROGRESS}.
	 *
	 * @param bugId bug identifier
	 * @param userId acting user identifier
	 * @param req    clarification payload
	 * @return updated bug details
	 */
	public BugDetailsDto clarifyBug(UUID bugId, UUID userId, ClarifyBugRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.TESTER)
			.getUser();

		if (bug.getStatus() != BugStatus.NEEDS_INFO) {
			throw new ConflictException("A bug can only be clarified in NEEDS_INFO status");
		}

		if (req.stepsToReproduce() != null) {
			String oldValue = bug.getStepsToReproduce();
			bug.setStepsToReproduce(req.stepsToReproduce());
			this.bugSupportService.recordHistory(bug, actor, STEPS_TO_REPRODUCE_FIELD, oldValue,
					req.stepsToReproduce());
		}
		if (req.environment() != null) {
			String oldValue = bug.getEnvironment();
			bug.setEnvironment(req.environment());
			this.bugSupportService.recordHistory(bug, actor, ENVIRONMENT_FIELD, oldValue, req.environment());
		}

		bug.setStatus(BugStatus.IN_PROGRESS);
		this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
				BugStatus.NEEDS_INFO.name(), BugStatus.IN_PROGRESS.name());
		this.bugSupportService.addSystemComment(bug, actor, req.comment());

		return saveAndMap(bug);
	}

	/**
	 * Submits a fix for an {@code IN_PROGRESS} or {@code RETURNED} bug and moves it to
	 * {@code IN_REVIEW}.
	 *
	 * @param bugId bug identifier
	 * @param userId acting user identifier
	 * @param req    fix payload, may be {@code null}
	 * @return updated bug details
	 */
	public BugDetailsDto fixBug(UUID bugId, UUID userId, FixBugRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.DEVELOPER)
			.getUser();

		if (bug.getStatus() != BugStatus.IN_PROGRESS && bug.getStatus() != BugStatus.RETURNED) {
			throw new ConflictException("A fix can only be submitted for a bug in IN_PROGRESS or RETURNED status");
		}

		if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(userId)) {
			throw new ForbiddenException("Only the assigned developer can submit a fix");
		}

		BugStatus oldStatus = bug.getStatus();
		bug.setStatus(BugStatus.IN_REVIEW);
		this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
				oldStatus.name(), BugStatus.IN_REVIEW.name());
		this.bugSupportService.addSystemComment(bug, actor, req != null ? req.comment() : null);

		return saveAndMap(bug);
	}

	/**
	 * Verifies a fix in review: either confirms and closes the bug as fixed, or returns it to the
	 * developer.
	 *
	 * @param bugId bug identifier
	 * @param userId acting user identifier
	 * @param req    verification payload
	 * @return updated bug details
	 */
	public BugDetailsDto verifyBug(UUID bugId, UUID userId, VerifyBugRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.TESTER)
			.getUser();

		if (bug.getStatus() != BugStatus.IN_REVIEW) {
			throw new ConflictException("A bug can only be verified in IN_REVIEW status");
		}

		BugStatus oldStatus = bug.getStatus();
		BugResolution oldResolution = bug.getResolution();

		if (req.decision() == VerifyDecision.CONFIRM) {
			bug.setStatus(BugStatus.CLOSED);
			bug.setResolution(BugResolution.FIXED);
			bug.setClosedAt(Instant.now());

			this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
					oldStatus.name(), BugStatus.CLOSED.name());
			this.bugSupportService.recordHistory(bug, actor, RESOLUTION_FIELD,
					nameOrNull(oldResolution), BugResolution.FIXED.name());
		}
		else if (req.decision() == VerifyDecision.RETURN) {
			bug.setStatus(BugStatus.RETURNED);
			bug.setResolution(null);

			this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
					oldStatus.name(), BugStatus.RETURNED.name());
			this.bugSupportService.recordHistory(bug, actor, RESOLUTION_FIELD,
					nameOrNull(oldResolution), null);
		}

		this.bugSupportService.addSystemComment(bug, actor, req.comment());

		return saveAndMap(bug);
	}

	/**
	 * Closes a bug without a fix using the supplied resolution. Closing as {@code FIXED} is
	 * reserved for the verification flow.
	 *
	 * @param bugId bug identifier
	 * @param userId acting user identifier
	 * @param req    closing payload
	 * @return updated bug details
	 */
	public BugDetailsDto closeWithoutFix(UUID bugId, UUID userId, CloseWithoutFixRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.TESTER)
			.getUser();

		if (bug.getStatus() == BugStatus.CLOSED) {
			throw new ConflictException("A closed bug cannot be closed again");
		}
		if (req.resolution() == BugResolution.FIXED) {
			throw new BadRequestException("Use verify endpoint to close as FIXED");
		}

		BugStatus oldStatus = bug.getStatus();
		BugResolution oldResolution = bug.getResolution();

		bug.setStatus(BugStatus.CLOSED);
		bug.setResolution(req.resolution());
		bug.setClosedAt(Instant.now());

		this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
				oldStatus.name(), BugStatus.CLOSED.name());
		this.bugSupportService.recordHistory(bug, actor, RESOLUTION_FIELD,
				nameOrNull(oldResolution), nameOrNull(req.resolution()));
		this.bugSupportService.addSystemComment(bug, actor, req.comment());

		return saveAndMap(bug);
	}

	/**
	 * Reopens a closed bug, clearing its resolution and closing timestamp.
	 *
	 * @param bugId bug identifier
	 * @param userId acting user identifier
	 * @param req    reopening payload
	 * @return updated bug details
	 */
	public BugDetailsDto reopenBug(UUID bugId, UUID userId, ReopenBugRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = bug.getProject().getId();
		this.projectAccessService.requireActiveProject(projectId);
		User actor = this.projectAccessService
			.requireRole(projectId, userId, ProjectRole.TESTER)
			.getUser();

		if (bug.getStatus() != BugStatus.CLOSED) {
			throw new ConflictException("Only a closed bug can be reopened");
		}

		BugResolution oldResolution = bug.getResolution();

		bug.setStatus(BugStatus.REOPENED);
		bug.setResolution(null);
		bug.setClosedAt(null);

		this.bugSupportService.recordHistory(bug, actor, STATUS_FIELD,
				BugStatus.CLOSED.name(), BugStatus.REOPENED.name());
		this.bugSupportService.recordHistory(bug, actor, RESOLUTION_FIELD,
				nameOrNull(oldResolution), null);
		this.bugSupportService.addSystemComment(bug, actor, req.comment());

		return saveAndMap(bug);
	}

	private BugDetailsDto saveAndMap(Bug bug) {
		Bug saved = this.bugRepository.save(bug);
		return this.bugSupportService.toDetailsDto(saved);
	}

	private static String nameOrNull(Enum<?> value) {
		return value == null ? null : value.name();
	}

}
