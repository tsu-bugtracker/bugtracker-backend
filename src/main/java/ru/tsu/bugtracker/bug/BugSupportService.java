package ru.tsu.bugtracker.bug;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.bug.dto.BugDetailsDto;
import ru.tsu.bugtracker.bug.dto.BugLinkDto;
import ru.tsu.bugtracker.bug.dto.BugSummaryDto;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.planning.FixPlanRepository;
import ru.tsu.bugtracker.planning.dto.FixPlanDto;
import ru.tsu.bugtracker.project.dto.LabelDto;
import ru.tsu.bugtracker.project.dto.SoftwareComponentDto;
import ru.tsu.bugtracker.release.dto.ReleaseShortDto;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;

/**
 * Shared helpers used by bug-related services: entity lookups, history and
 * system comment recording, overdue detection and DTO mapping.
 */
@Service
@Transactional
public class BugSupportService {

	private final BugRepository bugRepository;
	private final BugHistoryRepository bugHistoryRepository;
	private final BugLinkRepository bugLinkRepository;
	private final CommentRepository commentRepository;
	private final AttachmentRepository attachmentRepository;
	private final FixPlanRepository fixPlanRepository;
	private final UserRepository userRepository;

	public BugSupportService(
			BugRepository bugRepository,
			BugHistoryRepository bugHistoryRepository,
			BugLinkRepository bugLinkRepository,
			CommentRepository commentRepository,
			AttachmentRepository attachmentRepository,
			FixPlanRepository fixPlanRepository,
			UserRepository userRepository) {
		this.bugRepository = bugRepository;
		this.bugHistoryRepository = bugHistoryRepository;
		this.bugLinkRepository = bugLinkRepository;
		this.commentRepository = commentRepository;
		this.attachmentRepository = attachmentRepository;
		this.fixPlanRepository = fixPlanRepository;
		this.userRepository = userRepository;
	}

	public Bug requireBug(UUID bugId) {
		return bugRepository.findById(bugId)
				.orElseThrow(() -> new NotFoundException("Bug not found"));
	}

	public User requireUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found"));
	}

	public void recordHistory(Bug bug, User actor, String fieldName, String oldValue, String newValue) {
		if (Objects.equals(oldValue, newValue)) {
			return;
		}
		BugHistory history = new BugHistory();
		history.setBug(bug);
		history.setActor(actor);
		history.setFieldName(fieldName);
		history.setOldValue(oldValue);
		history.setNewValue(newValue);
		bugHistoryRepository.save(history);
	}

	public void addSystemComment(Bug bug, User author, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		Comment comment = new Comment();
		comment.setBug(bug);
		comment.setAuthor(author);
		comment.setBody(text.trim());
		commentRepository.save(comment);
	}

	public boolean isOverdue(Bug bug) {
		return bug.getDeadline() != null
				&& bug.getStatus() != BugStatus.CLOSED
				&& bug.getDeadline().isBefore(Instant.now());
	}

	public BugSummaryDto toSummaryDto(Bug bug) {
		return new BugSummaryDto(
				bug.getId(),
				bug.getProject().getId(),
				bug.getBugKey(),
				bug.getTitle(),
				bug.getStatus(),
				bug.getPriority(),
				bug.getRecommendedPriority(),
				bug.getResolution(),
				UserShortDto.from(bug.getAuthor()),
				bug.getAssignee() == null ? null : UserShortDto.from(bug.getAssignee()),
				bug.getComponent() == null ? null : SoftwareComponentDto.from(bug.getComponent()),
				bug.getLabels().stream().map(LabelDto::from).toList(),
				ReleaseShortDto.from(bug.getTargetRelease()),
				bug.getDeadline(),
				isOverdue(bug),
				bug.getCreatedAt(),
				bug.getUpdatedAt()
		);
	}

	public BugDetailsDto toDetailsDto(Bug bug) {
		FixPlanDto fixPlan = fixPlanRepository.findByBugId(bug.getId())
				.map(FixPlanDto::from)
				.orElse(null);
		List<BugLinkDto> links = bugLinkRepository.findAllBySourceBugIdOrderByCreatedAtAsc(bug.getId())
				.stream()
				.map(BugLinkDto::from)
				.toList();
		long commentsCount = commentRepository.countByBugId(bug.getId());
		long attachmentsCount = attachmentRepository.countByBugId(bug.getId());

		return new BugDetailsDto(
				bug.getId(),
				bug.getProject().getId(),
				bug.getBugKey(),
				bug.getTitle(),
				bug.getDescription(),
				bug.getStepsToReproduce(),
				bug.getEnvironment(),
				bug.getExpectedResult(),
				bug.getActualResult(),
				bug.getStatus(),
				bug.getPriority(),
				bug.getRecommendedPriority(),
				bug.getResolution(),
				UserShortDto.from(bug.getAuthor()),
				bug.getAssignee() == null ? null : UserShortDto.from(bug.getAssignee()),
				bug.getComponent() == null ? null : SoftwareComponentDto.from(bug.getComponent()),
				bug.getLabels().stream().map(LabelDto::from).toList(),
				fixPlan,
				ReleaseShortDto.from(bug.getTargetRelease()),
				ReleaseShortDto.from(bug.getFixedInRelease()),
				List.of(),
				links,
				commentsCount,
				attachmentsCount,
				bug.getDeadline(),
				isOverdue(bug),
				bug.getCreatedAt(),
				bug.getUpdatedAt()
		);
	}

}
