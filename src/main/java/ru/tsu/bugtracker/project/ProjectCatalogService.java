package ru.tsu.bugtracker.project;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugRepository;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.project.dto.CreateComponentRequest;
import ru.tsu.bugtracker.project.dto.CreateLabelRequest;
import ru.tsu.bugtracker.project.dto.LabelDto;
import ru.tsu.bugtracker.project.dto.MergeLabelsRequest;
import ru.tsu.bugtracker.project.dto.SoftwareComponentDto;
import ru.tsu.bugtracker.project.dto.UpdateComponentRequest;
import ru.tsu.bugtracker.project.dto.UpdateLabelRequest;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;

/**
 * Project-scoped catalog operations for software components and labels.
 */
@Service
@Transactional
public class ProjectCatalogService {

	private static final String COMPONENT_NOT_FOUND = "Component not found";

	private static final String COMPONENT_NAME_EXISTS = "A component with this name already exists";

	private static final String LABEL_NOT_FOUND = "Label not found";

	private static final String LABEL_NAME_EXISTS = "A label with this name already exists";

	private static final String OWNER_NOT_MEMBER = "Owner developer must be a member of the project";

	private static final String OWNER_NOT_FOUND = "Owner developer not found";

	private static final String EMPTY_MERGE_SOURCES = "At least one source label must be different from the target label";

	private final ProjectAccessService projectAccessService;

	private final SoftwareComponentRepository softwareComponentRepository;

	private final LabelRepository labelRepository;

	private final ProjectMemberRepository projectMemberRepository;

	private final UserRepository userRepository;

	private final BugRepository bugRepository;

	public ProjectCatalogService(
			ProjectAccessService projectAccessService,
			SoftwareComponentRepository softwareComponentRepository,
			LabelRepository labelRepository,
			ProjectMemberRepository projectMemberRepository,
			UserRepository userRepository,
			BugRepository bugRepository) {
		this.projectAccessService = projectAccessService;
		this.softwareComponentRepository = softwareComponentRepository;
		this.labelRepository = labelRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.userRepository = userRepository;
		this.bugRepository = bugRepository;
	}

	/**
	 * Returns all software components of a project ordered by name.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @return project components
	 */
	@Transactional(readOnly = true)
	public List<SoftwareComponentDto> listComponents(UUID projectId, UUID userId) {
		this.projectAccessService.requireMember(projectId, userId);
		return this.softwareComponentRepository.findAllByProjectIdOrderByNameAsc(projectId).stream()
				.map(SoftwareComponentDto::from)
				.toList();
	}

	/**
	 * Creates a software component in a project.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @param request   component details
	 * @return created component
	 */
	public SoftwareComponentDto createComponent(UUID projectId, UUID userId, CreateComponentRequest request) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		if (this.softwareComponentRepository.existsByProjectIdAndNameIgnoreCase(projectId, request.name())) {
			throw new ConflictException(COMPONENT_NAME_EXISTS);
		}

		SoftwareComponent component = new SoftwareComponent();
		component.setProject(project);
		component.setName(request.name());
		component.setDescription(request.description());
		if (request.ownerDeveloperId() != null) {
			component.setOwnerDeveloper(resolveOwnerDeveloper(projectId, request.ownerDeveloperId()));
		}

		return SoftwareComponentDto.from(this.softwareComponentRepository.save(component));
	}

	/**
	 * Updates a software component of a project.
	 *
	 * @param projectId   project identifier
	 * @param componentId component identifier
	 * @param userId      current user identifier
	 * @param request     component changes
	 * @return updated component
	 */
	public SoftwareComponentDto updateComponent(UUID projectId, UUID componentId, UUID userId,
			UpdateComponentRequest request) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		SoftwareComponent component = this.softwareComponentRepository
				.findByIdAndProjectId(componentId, projectId)
				.orElseThrow(() -> new NotFoundException(COMPONENT_NOT_FOUND));

		if (request.name() != null) {
			if (!request.name().equalsIgnoreCase(component.getName())
					&& this.softwareComponentRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(
							projectId, request.name(), componentId)) {
				throw new ConflictException(COMPONENT_NAME_EXISTS);
			}
			component.setName(request.name());
		}
		if (request.description() != null) {
			component.setDescription(request.description());
		}
		if (request.ownerDeveloperId() != null) {
			component.setOwnerDeveloper(resolveOwnerDeveloper(projectId, request.ownerDeveloperId()));
		}

		return SoftwareComponentDto.from(this.softwareComponentRepository.save(component));
	}

	/**
	 * Deletes a software component and detaches it from all bugs.
	 *
	 * @param projectId   project identifier
	 * @param componentId component identifier
	 * @param userId      current user identifier
	 */
	public void deleteComponent(UUID projectId, UUID componentId, UUID userId) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		SoftwareComponent component = this.softwareComponentRepository
				.findByIdAndProjectId(componentId, projectId)
				.orElseThrow(() -> new NotFoundException(COMPONENT_NOT_FOUND));

		this.bugRepository.clearComponent(componentId);
		this.softwareComponentRepository.delete(component);
	}

	/**
	 * Returns all labels of a project ordered by name.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @return project labels
	 */
	@Transactional(readOnly = true)
	public List<LabelDto> listLabels(UUID projectId, UUID userId) {
		this.projectAccessService.requireMember(projectId, userId);
		return this.labelRepository.findAllByProjectIdOrderByNameAsc(projectId).stream()
				.map(LabelDto::from)
				.toList();
	}

	/**
	 * Creates a label in a project.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @param request   label details
	 * @return created label
	 */
	public LabelDto createLabel(UUID projectId, UUID userId, CreateLabelRequest request) {
		Project project = this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId,
				ProjectRole.TEAM_LEAD, ProjectRole.DEVELOPER, ProjectRole.TESTER);

		if (this.labelRepository.existsByProjectIdAndNameIgnoreCase(projectId, request.name())) {
			throw new ConflictException(LABEL_NAME_EXISTS);
		}

		Label label = new Label();
		label.setProject(project);
		label.setName(request.name());
		label.setColor(request.color());

		return LabelDto.from(this.labelRepository.save(label));
	}

	/**
	 * Updates a label of a project.
	 *
	 * @param projectId project identifier
	 * @param labelId   label identifier
	 * @param userId    current user identifier
	 * @param request   label changes
	 * @return updated label
	 */
	public LabelDto updateLabel(UUID projectId, UUID labelId, UUID userId, UpdateLabelRequest request) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		Label label = this.labelRepository.findByIdAndProjectId(labelId, projectId)
				.orElseThrow(() -> new NotFoundException(LABEL_NOT_FOUND));

		if (request.name() != null) {
			if (!request.name().equalsIgnoreCase(label.getName())
					&& this.labelRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(
							projectId, request.name(), labelId)) {
				throw new ConflictException(LABEL_NAME_EXISTS);
			}
			label.setName(request.name());
		}
		if (request.color() != null) {
			label.setColor(request.color());
		}

		return LabelDto.from(this.labelRepository.save(label));
	}

	/**
	 * Deletes a label and removes it from all bugs that use it.
	 *
	 * @param projectId project identifier
	 * @param labelId   label identifier
	 * @param userId    current user identifier
	 */
	public void deleteLabel(UUID projectId, UUID labelId, UUID userId) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		Label label = this.labelRepository.findByIdAndProjectId(labelId, projectId)
				.orElseThrow(() -> new NotFoundException(LABEL_NOT_FOUND));

		List<Bug> bugs = this.bugRepository.findAllByProjectIdAndLabelsId(projectId, labelId);
		bugs.forEach(bug -> bug.getLabels().remove(label));

		this.labelRepository.delete(label);
	}

	/**
	 * Merges the given source labels into a target label, moving all bug links and deleting
	 * the source labels. The target label is ignored when it is also listed as a source.
	 *
	 * @param projectId project identifier
	 * @param userId    current user identifier
	 * @param request   merge request
	 * @return target label
	 */
	public LabelDto mergeLabels(UUID projectId, UUID userId, MergeLabelsRequest request) {
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId, ProjectRole.TEAM_LEAD);

		UUID targetLabelId = request.targetLabelId();
		Label targetLabel = this.labelRepository.findByIdAndProjectId(targetLabelId, projectId)
				.orElseThrow(() -> new NotFoundException(LABEL_NOT_FOUND));

		if (request.sourceLabelIds() == null) {
			throw new BadRequestException(EMPTY_MERGE_SOURCES);
		}

		List<Label> sourceLabels = new ArrayList<>();
		for (UUID sourceLabelId : new LinkedHashSet<>(request.sourceLabelIds())) {
			if (sourceLabelId == null) {
				throw new BadRequestException(EMPTY_MERGE_SOURCES);
			}
			if (sourceLabelId.equals(targetLabelId)) {
				continue;
			}
			Label sourceLabel = this.labelRepository.findByIdAndProjectId(sourceLabelId, projectId)
					.orElseThrow(() -> new NotFoundException(LABEL_NOT_FOUND));
			sourceLabels.add(sourceLabel);
		}

		for (Label sourceLabel : sourceLabels) {
			List<Bug> bugs = this.bugRepository
					.findAllByProjectIdAndLabelsId(projectId, sourceLabel.getId());
			for (Bug bug : bugs) {
				bug.getLabels().remove(sourceLabel);
				bug.getLabels().add(targetLabel);
			}
			this.labelRepository.delete(sourceLabel);
		}

		return LabelDto.from(targetLabel);
	}

	private User resolveOwnerDeveloper(UUID projectId, UUID ownerDeveloperId) {
		User owner = this.userRepository.findById(ownerDeveloperId)
				.orElseThrow(() -> new BadRequestException(OWNER_NOT_FOUND));
		if (!this.projectMemberRepository.existsByProjectIdAndUserId(projectId, ownerDeveloperId)) {
			throw new BadRequestException(OWNER_NOT_MEMBER);
		}
		return owner;
	}

}
