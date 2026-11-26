package ru.tsu.bugtracker.project;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.common.exception.NotFoundException;

/**
 * Resolves projects and enforces project membership and role based access rules.
 */
@Service
@Transactional(readOnly = true)
public class ProjectAccessService {

	private static final String PROJECT_NOT_FOUND = "Project not found";

	private static final String PROJECT_ARCHIVED = "Project is archived";

	private static final String ACCESS_DENIED = "Access to project denied";

	private static final String INSUFFICIENT_PERMISSIONS = "Insufficient project permissions";

	private final ProjectRepository projectRepository;

	private final ProjectMemberRepository projectMemberRepository;

	public ProjectAccessService(ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository) {
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
	}

	/**
	 * Returns the project with the given identifier.
	 *
	 * @param projectId project identifier
	 * @return existing project
	 * @throws NotFoundException when the project does not exist
	 */
	public Project requireProject(UUID projectId) {
		return this.projectRepository.findById(projectId)
			.orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND));
	}

	/**
	 * Returns the project with the given identifier, ensuring it is not archived.
	 *
	 * @param projectId project identifier
	 * @return existing project that is not archived
	 * @throws NotFoundException when the project does not exist
	 * @throws ConflictException when the project is archived
	 */
	public Project requireActiveProject(UUID projectId) {
		Project project = requireProject(projectId);
		if (project.getStatus() == ProjectStatus.ARCHIVED) {
			throw new ConflictException(PROJECT_ARCHIVED);
		}
		return project;
	}

	/**
	 * Returns the membership of the given user in the given project.
	 *
	 * @param projectId project identifier
	 * @param userId    user identifier
	 * @return project membership
	 * @throws NotFoundException  when the project does not exist
	 * @throws ForbiddenException when the user is not a member of the project
	 */
	public ProjectMember requireMember(UUID projectId, UUID userId) {
		requireProject(projectId);
		return this.projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
			.orElseThrow(() -> new ForbiddenException(ACCESS_DENIED));
	}

	/**
	 * Returns the membership of the given user in the given project, ensuring the member has
	 * one of the allowed roles.
	 *
	 * @param projectId    project identifier
	 * @param userId       user identifier
	 * @param allowedRoles roles that are allowed to perform the action
	 * @return project membership with an allowed role
	 * @throws NotFoundException  when the project does not exist
	 * @throws ForbiddenException when the user is not a member or has a different role
	 */
	public ProjectMember requireRole(UUID projectId, UUID userId, ProjectRole... allowedRoles) {
		ProjectMember member = requireMember(projectId, userId);
		if (allowedRoles == null || !Arrays.asList(allowedRoles).contains(member.getRole())) {
			throw new ForbiddenException(INSUFFICIENT_PERMISSIONS);
		}
		return member;
	}

	/**
	 * Checks whether the project has at least one active tester.
	 *
	 * @param projectId project identifier
	 * @return {@code true} when an active tester exists
	 */
	public boolean hasActiveTester(UUID projectId) {
		return this.projectMemberRepository
			.countByProjectIdAndRoleAndStatus(projectId, ProjectRole.TESTER, MemberStatus.ACTIVE) > 0;
	}

	/**
	 * Recalculates whether bug tracking is enabled for the project based on the presence of
	 * active testers.
	 *
	 * @param project project to recalculate the flag for
	 * @return the new value of the bug tracking flag
	 */
	@Transactional
	public boolean recalculateBugTrackingEnabled(Project project) {
		boolean enabled = hasActiveTester(project.getId());
		project.setBugTrackingEnabled(enabled);
		return enabled;
	}

}
