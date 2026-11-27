package ru.tsu.bugtracker.project;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.bug.BugStatus;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.git.GitIntegrationService;
import ru.tsu.bugtracker.project.dto.CreateProjectRequest;
import ru.tsu.bugtracker.project.dto.ProjectDetailsDto;
import ru.tsu.bugtracker.project.dto.ProjectSummaryDto;
import ru.tsu.bugtracker.project.dto.UpdateProjectRequest;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

/**
 * Implements project lifecycle operations.
 */
@Service
@Transactional
public class ProjectService {

	private static final String USER_NOT_FOUND = "User not found";

	private static final String PROJECT_NAME_CONFLICT = "Project name is already in use";

	private static final String PROJECT_ALREADY_ARCHIVED = "Project is already archived";

	private static final String INVALID_DATE_RANGE = "End date must not be before start date";

	private static final String CANNOT_CREATE_PROJECTS = "User is not allowed to create projects";

	private static final String PROJECT_TARGET_TYPE = "PROJECT";

	private static final String PROJECT_CREATED_ACTION = "PROJECT_CREATED";

	private static final String PROJECT_ARCHIVED_ACTION = "PROJECT_ARCHIVED";

	private final ProjectRepository projectRepository;

	private final ProjectMemberRepository projectMemberRepository;

	private final ProjectAccessService projectAccessService;

	private final BugRepository bugRepository;

	private final UserRepository userRepository;

	private final AuditService auditService;

	private final GitIntegrationService gitIntegrationService;

	public ProjectService(ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectAccessService projectAccessService,
			BugRepository bugRepository,
			UserRepository userRepository,
			AuditService auditService,
			GitIntegrationService gitIntegrationService) {
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectAccessService = projectAccessService;
		this.bugRepository = bugRepository;
		this.userRepository = userRepository;
		this.auditService = auditService;
		this.gitIntegrationService = gitIntegrationService;
	}

	/**
	 * Returns projects the user belongs to, optionally filtered by status and name.
	 *
	 * @param userId   current user identifier
	 * @param status   project status filter, or {@code null}
	 * @param search   case-insensitive name fragment, or {@code null}
	 * @param pageable pagination
	 * @return page of project summaries
	 */
	@Transactional(readOnly = true)
	public PagedResponse<ProjectSummaryDto> listProjects(UUID userId, ProjectStatus status, String search,
			Pageable pageable) {
		List<ProjectMember> memberships = this.projectMemberRepository.findAllByUserId(userId);
		if (memberships.isEmpty()) {
			return new PagedResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
		}

		Map<UUID, ProjectRole> roles = new HashMap<>();
		for (ProjectMember membership : memberships) {
			roles.put(membership.getProject().getId(), membership.getRole());
		}
		List<UUID> projectIds = new ArrayList<>(roles.keySet());
		Specification<Project> specification = buildListProjectsSpecification(projectIds, status, search);

		Page<ProjectSummaryDto> page = this.projectRepository.findAll(specification, pageable)
			.map(project -> toSummaryDto(project, roles.get(project.getId())));
		return PagedResponse.from(page);
	}

	private Specification<Project> buildListProjectsSpecification(
			List<UUID> projectIds, ProjectStatus status, String search) {
		return (root, _, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(root.get("id").in(projectIds));
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	/**
	 * Creates a project owned by the current user.
	 *
	 * @param userId  current user identifier
	 * @param request project data
	 * @return created project details
	 */
	public ProjectDetailsDto createProject(UUID userId, CreateProjectRequest request) {
		User user = requireUser(userId);
		if (user.getStatus() != UserStatus.ACTIVE || !user.isCanCreateProjects()) {
			throw new ForbiddenException(CANNOT_CREATE_PROJECTS);
		}
		if (this.projectRepository.existsByNameIgnoreCase(request.name())) {
			throw new ConflictException(PROJECT_NAME_CONFLICT);
		}
		validateDateRange(request.startDate(), request.endDate());

		Project project = new Project();
		project.setName(request.name());
		project.setDescription(request.description());
		project.setTechStack(request.techStack());
		project.setStatus(ProjectStatus.ACTIVE);
		project.setBugTrackingEnabled(false);
		project.setBugSequence(0L);
		project.setStartDate(request.startDate());
		project.setEndDate(request.endDate());
		project.setOwner(user);
		project = this.projectRepository.save(project);

		ProjectMember member = new ProjectMember();
		member.setProject(project);
		member.setUser(user);
		member.setRole(ProjectRole.TEAM_LEAD);
		member.setStatus(MemberStatus.ACTIVE);
		this.projectMemberRepository.save(member);

		if (request.gitRepositoryUrl() != null && !request.gitRepositoryUrl().isBlank()) {
			this.gitIntegrationService.connectFromProjectCreation(project, request.gitRepositoryUrl());
		}

		this.auditService.log(user, PROJECT_CREATED_ACTION, PROJECT_TARGET_TYPE, project.getId(),
				Map.of("name", project.getName()));

		return getProject(project.getId(), userId);
	}

	/**
	 * Returns details of a project the user belongs to.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @return project details
	 */
	@Transactional(readOnly = true)
	public ProjectDetailsDto getProject(UUID projectId, UUID userId) {
		ProjectMember member = this.projectAccessService.requireMember(projectId, userId);
		return toDetailsDto(member.getProject(), member.getRole());
	}

	/**
	 * Updates an active project. Only a team lead may change it.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @param request   project changes
	 * @return updated project details
	 */
	public ProjectDetailsDto updateProject(UUID projectId, UUID userId, UpdateProjectRequest request) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (request.name() != null && !request.name().equals(project.getName())
				&& this.projectRepository.existsByNameIgnoreCaseAndIdNot(request.name(), projectId)) {
			throw new ConflictException(PROJECT_NAME_CONFLICT);
		}
		if (request.name() != null) {
			project.setName(request.name());
		}
		if (request.description() != null) {
			project.setDescription(request.description());
		}
		if (request.techStack() != null) {
			project.setTechStack(request.techStack());
		}
		if (request.startDate() != null) {
			project.setStartDate(request.startDate());
		}
		if (request.endDate() != null) {
			project.setEndDate(request.endDate());
		}
		validateDateRange(project.getStartDate(), project.getEndDate());
		this.projectRepository.save(project);

		return getProject(projectId, userId);
	}

	/**
	 * Archives a project. Only a team lead may archive it.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @return archived project details
	 */
	public ProjectDetailsDto archiveProject(UUID projectId, UUID userId) {
		Project project = this.projectAccessService.requireProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);
		if (project.getStatus() == ProjectStatus.ARCHIVED) {
			throw new ConflictException(PROJECT_ALREADY_ARCHIVED);
		}

		User actor = requireUser(userId);
		project.setStatus(ProjectStatus.ARCHIVED);
		this.projectRepository.save(project);

		this.auditService.log(actor, PROJECT_ARCHIVED_ACTION, PROJECT_TARGET_TYPE, project.getId(),
				Map.of("name", project.getName()));

		return getProject(projectId, userId);
	}

	private ProjectSummaryDto toSummaryDto(Project project, ProjectRole myRole) {
		return new ProjectSummaryDto(
				project.getId(),
				project.getName(),
				project.getDescription(),
				project.getTechStack(),
				project.getStatus(),
				project.isBugTrackingEnabled(),
				myRole,
				this.bugRepository.countByProjectIdAndStatusNot(project.getId(), BugStatus.CLOSED),
				this.projectMemberRepository.countByProjectId(project.getId()),
				project.getStartDate(),
				project.getEndDate(),
				project.getCreatedAt()
		);
	}

	private ProjectDetailsDto toDetailsDto(Project project, ProjectRole myRole) {
		return new ProjectDetailsDto(
				project.getId(),
				project.getName(),
				project.getDescription(),
				project.getTechStack(),
				project.getStatus(),
				project.isBugTrackingEnabled(),
				this.projectAccessService.hasActiveTester(project.getId()),
				UserShortDto.from(project.getOwner()),
				myRole,
				this.gitIntegrationService.findConnectedByProjectId(project.getId()),
				project.getStartDate(),
				project.getEndDate(),
				this.projectMemberRepository.countByProjectId(project.getId()),
				project.getCreatedAt(),
				project.getUpdatedAt()
		);
	}

	private User requireUser(UUID userId) {
		return this.userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw new BadRequestException(INVALID_DATE_RANGE);
		}
	}

}
