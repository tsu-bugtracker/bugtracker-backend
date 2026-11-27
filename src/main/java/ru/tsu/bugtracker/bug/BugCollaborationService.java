package ru.tsu.bugtracker.bug;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ru.tsu.bugtracker.bug.dto.AttachmentDto;
import ru.tsu.bugtracker.bug.dto.BugLinkDto;
import ru.tsu.bugtracker.bug.dto.CommentDto;
import ru.tsu.bugtracker.bug.dto.CreateBugLinkRequest;
import ru.tsu.bugtracker.bug.dto.CreateCommentRequest;
import ru.tsu.bugtracker.bug.dto.UpdateCommentRequest;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.project.ProjectAccessService;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.storage.S3StorageService;

/**
 * Collaboration features of a bug: comments, attachments and links between bugs.
 */
@Service
@Transactional
public class BugCollaborationService {

	private static final String COMMENT_NOT_FOUND = "Comment not found";

	private static final String ATTACHMENT_NOT_FOUND = "Attachment not found";

	private static final String BUG_LINK_NOT_FOUND = "Bug link not found";

	private static final String EMPTY_FILE = "File must not be empty";

	private static final String SELF_LINK = "Cannot link bug to itself";

	private static final String CROSS_PROJECT_LINK = "Both bugs must belong to the same project";

	private static final String DUPLICATE_LINK = "Bug link already exists";

	private static final String COMMENT_EDIT_FORBIDDEN = "Only the comment author can edit this comment";

	private static final String COMMENT_DELETE_FORBIDDEN =
			"Only the comment author or a team lead can delete this comment";

	private static final String ATTACHMENT_DELETE_FORBIDDEN =
			"Only the attachment uploader or a team lead can delete this attachment";

	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

	private static final String DEFAULT_FILE_NAME = "attachment";

	private final BugSupportService bugSupportService;

	private final CommentRepository commentRepository;

	private final AttachmentRepository attachmentRepository;

	private final BugLinkRepository bugLinkRepository;

	private final ProjectAccessService projectAccessService;

	private final S3StorageService s3StorageService;

	public BugCollaborationService(
			BugSupportService bugSupportService,
			CommentRepository commentRepository,
			AttachmentRepository attachmentRepository,
			BugLinkRepository bugLinkRepository,
			ProjectAccessService projectAccessService,
			S3StorageService s3StorageService) {
		this.bugSupportService = bugSupportService;
		this.commentRepository = commentRepository;
		this.attachmentRepository = attachmentRepository;
		this.bugLinkRepository = bugLinkRepository;
		this.projectAccessService = projectAccessService;
		this.s3StorageService = s3StorageService;
	}

	/**
	 * Returns a page of comments of the given bug.
	 *
	 * @param bugId    bug identifier
	 * @param userId   requesting user identifier
	 * @param pageable pagination information
	 * @return page of comments
	 */
	public PagedResponse<CommentDto> listComments(UUID bugId, UUID userId, Pageable pageable) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		this.projectAccessService.requireMember(projectIdOf(bug), userId);
		return PagedResponse.from(this.commentRepository
			.findAllByBugIdOrderByCreatedAtAsc(bugId, pageable)
			.map(this::toCommentDto));
	}

	/**
	 * Adds a comment to the given bug.
	 *
	 * @param bugId  bug identifier
	 * @param userId author identifier
	 * @param req    comment payload
	 * @return created comment
	 */
	public CommentDto createComment(UUID bugId, UUID userId, CreateCommentRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = projectIdOf(bug);
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId,
				ProjectRole.TESTER, ProjectRole.DEVELOPER, ProjectRole.TEAM_LEAD);

		Comment comment = new Comment();
		comment.setBug(bug);
		comment.setAuthor(this.bugSupportService.requireUser(userId));
		comment.setBody(req.text().trim());
		return toCommentDto(this.commentRepository.save(comment));
	}

	/**
	 * Updates the body of a comment. Only the author may edit the comment.
	 *
	 * @param bugId     bug identifier
	 * @param commentId comment identifier
	 * @param userId    requesting user identifier
	 * @param req       comment payload
	 * @return updated comment
	 */
	public CommentDto updateComment(UUID bugId, UUID commentId, UUID userId, UpdateCommentRequest req) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		this.projectAccessService.requireActiveProject(projectIdOf(bug));

		Comment comment = this.commentRepository.findByIdAndBugId(commentId, bugId)
			.orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND));
		if (!comment.getAuthor().getId().equals(userId)) {
			throw new ForbiddenException(COMMENT_EDIT_FORBIDDEN);
		}

		comment.setBody(req.text().trim());
		return toCommentDto(this.commentRepository.save(comment));
	}

	/**
	 * Deletes a comment. Only its author or a team lead may delete the comment.
	 *
	 * @param bugId     bug identifier
	 * @param commentId comment identifier
	 * @param userId    requesting user identifier
	 */
	public void deleteComment(UUID bugId, UUID commentId, UUID userId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = projectIdOf(bug);
		this.projectAccessService.requireActiveProject(projectId);

		Comment comment = this.commentRepository.findByIdAndBugId(commentId, bugId)
			.orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND));
		ProjectMember member = this.projectAccessService.requireMember(projectId, userId);
		boolean teamLead = member.getRole() == ProjectRole.TEAM_LEAD;
		if (!comment.getAuthor().getId().equals(userId) && !teamLead) {
			throw new ForbiddenException(COMMENT_DELETE_FORBIDDEN);
		}

		List<Attachment> attachments = this.attachmentRepository
			.findAllByCommentIdOrderByCreatedAtAsc(comment.getId());
		for (Attachment attachment : attachments) {
			attachment.setComment(null);
		}
		this.attachmentRepository.saveAll(attachments);
		this.commentRepository.delete(comment);
	}

	/**
	 * Returns all attachments of the given bug.
	 *
	 * @param bugId  bug identifier
	 * @param userId requesting user identifier
	 * @return bug attachments
	 */
	public List<AttachmentDto> listAttachments(UUID bugId, UUID userId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		this.projectAccessService.requireMember(projectIdOf(bug), userId);
		return this.attachmentRepository.findAllByBugIdOrderByCreatedAtAsc(bugId).stream()
			.map(AttachmentDto::from)
			.toList();
	}

	/**
	 * Uploads a file and attaches it to the given bug and, optionally, to a comment.
	 *
	 * @param bugId     bug identifier
	 * @param userId    uploading user identifier
	 * @param file      uploaded file
	 * @param commentId optional comment identifier
	 * @return created attachment
	 */
	public AttachmentDto uploadAttachment(UUID bugId, UUID userId, MultipartFile file, UUID commentId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = projectIdOf(bug);
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId,
				ProjectRole.TESTER, ProjectRole.DEVELOPER, ProjectRole.TEAM_LEAD);

		if (file == null || file.isEmpty()) {
			throw new BadRequestException(EMPTY_FILE);
		}

		Comment comment = null;
		if (commentId != null) {
			comment = this.commentRepository.findByIdAndBugId(commentId, bugId)
				.orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND));
		}

		String fileName = normalizeFileName(file.getOriginalFilename());
		String contentType = normalizeContentType(file.getContentType());
		String objectKey = "bugs/" + bugId + "/" + UUID.randomUUID() + "_" + safeFileName(fileName);

		try (InputStream inputStream = file.getInputStream()) {
			this.s3StorageService.upload(objectKey, inputStream, file.getSize(), contentType);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read uploaded file", ex);
		}

		Attachment attachment = new Attachment();
		attachment.setBug(bug);
		attachment.setComment(comment);
		attachment.setFileName(fileName);
		attachment.setContentType(contentType);
		attachment.setSizeBytes(file.getSize());
		attachment.setS3ObjectKey(objectKey);
		attachment.setUploadedBy(this.bugSupportService.requireUser(userId));
		return AttachmentDto.from(this.attachmentRepository.save(attachment));
	}

	/**
	 * Downloads the content of an attachment.
	 *
	 * @param attachmentId attachment identifier
	 * @param userId       requesting user identifier
	 * @return attachment metadata together with its content
	 */
	public AttachmentDownload downloadAttachment(UUID attachmentId, UUID userId) {
		Attachment attachment = this.attachmentRepository.findById(attachmentId)
			.orElseThrow(() -> new NotFoundException(ATTACHMENT_NOT_FOUND));
		this.projectAccessService.requireMember(projectIdOf(attachment.getBug()), userId);
		byte[] data = this.s3StorageService.download(attachment.getS3ObjectKey());
		return new AttachmentDownload(attachment.getFileName(), attachment.getContentType(), data);
	}

	/**
	 * Deletes an attachment. Only its uploader or a team lead may delete it.
	 *
	 * @param attachmentId attachment identifier
	 * @param userId       requesting user identifier
	 */
	public void deleteAttachment(UUID attachmentId, UUID userId) {
		Attachment attachment = this.attachmentRepository.findById(attachmentId)
			.orElseThrow(() -> new NotFoundException(ATTACHMENT_NOT_FOUND));
		UUID projectId = projectIdOf(attachment.getBug());
		this.projectAccessService.requireActiveProject(projectId);

		ProjectMember member = this.projectAccessService.requireMember(projectId, userId);
		boolean teamLead = member.getRole() == ProjectRole.TEAM_LEAD;
		if (!attachment.getUploadedBy().getId().equals(userId) && !teamLead) {
			throw new ForbiddenException(ATTACHMENT_DELETE_FORBIDDEN);
		}

		this.s3StorageService.delete(attachment.getS3ObjectKey());
		this.attachmentRepository.delete(attachment);
	}

	/**
	 * Creates a link from the given bug to another bug of the same project.
	 *
	 * @param bugId  source bug identifier
	 * @param userId creating user identifier
	 * @param req    link payload
	 * @return created link
	 */
	public BugLinkDto linkBugs(UUID bugId, UUID userId, CreateBugLinkRequest req) {
		Bug sourceBug = this.bugSupportService.requireBug(bugId);
		UUID projectId = projectIdOf(sourceBug);
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId,
				ProjectRole.TESTER, ProjectRole.DEVELOPER, ProjectRole.TEAM_LEAD);

		if (bugId.equals(req.targetBugId())) {
			throw new BadRequestException(SELF_LINK);
		}

		Bug targetBug = this.bugSupportService.requireBug(req.targetBugId());
		if (!projectIdOf(targetBug).equals(projectId)) {
			throw new BadRequestException(CROSS_PROJECT_LINK);
		}

		if (this.bugLinkRepository.existsBySourceBugIdAndTargetBugIdAndLinkType(
				bugId, req.targetBugId(), req.linkType())) {
			throw new ConflictException(DUPLICATE_LINK);
		}

		BugLink link = new BugLink();
		link.setSourceBug(sourceBug);
		link.setTargetBug(targetBug);
		link.setLinkType(req.linkType());
		link.setCreatedBy(this.bugSupportService.requireUser(userId));
		return BugLinkDto.from(this.bugLinkRepository.save(link));
	}

	/**
	 * Removes a link originating from the given bug.
	 *
	 * @param bugId  source bug identifier
	 * @param linkId link identifier
	 * @param userId requesting user identifier
	 */
	public void unlinkBugs(UUID bugId, UUID linkId, UUID userId) {
		Bug bug = this.bugSupportService.requireBug(bugId);
		UUID projectId = projectIdOf(bug);
		this.projectAccessService.requireActiveProject(projectId);
		this.projectAccessService.requireRole(projectId, userId,
				ProjectRole.TESTER, ProjectRole.DEVELOPER, ProjectRole.TEAM_LEAD);

		BugLink link = this.bugLinkRepository.findByIdAndSourceBugId(linkId, bugId)
			.orElseThrow(() -> new NotFoundException(BUG_LINK_NOT_FOUND));
		this.bugLinkRepository.delete(link);
	}

	private CommentDto toCommentDto(Comment comment) {
		List<AttachmentDto> attachments = this.attachmentRepository
			.findAllByCommentIdOrderByCreatedAtAsc(comment.getId()).stream()
			.map(AttachmentDto::from)
			.toList();
		return new CommentDto(
				comment.getId(),
				comment.getBug().getId(),
				UserShortDto.from(comment.getAuthor()),
				comment.getBody(),
				attachments,
				comment.getCreatedAt(),
				comment.getUpdatedAt()
		);
	}

	private static UUID projectIdOf(Bug bug) {
		return bug.getProject().getId();
	}

	private static String normalizeFileName(String originalFileName) {
		return originalFileName == null || originalFileName.isBlank()
				? DEFAULT_FILE_NAME
				: originalFileName;
	}

	private static String normalizeContentType(String contentType) {
		return contentType == null || contentType.isBlank()
				? DEFAULT_CONTENT_TYPE
				: contentType;
	}

	private static String safeFileName(String fileName) {
		String normalized = fileName.replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		if (slash >= 0) {
			normalized = normalized.substring(slash + 1);
		}
		normalized = normalized.replaceAll("[\\r\\n\\t]", "_").trim();
		return normalized.isEmpty() ? DEFAULT_FILE_NAME : normalized;
	}

	/**
	 * Content of a downloaded attachment.
	 *
	 * @param fileName    original file name
	 * @param contentType MIME content type
	 * @param data        file bytes
	 */
	public record AttachmentDownload(String fileName, String contentType, byte[] data) {
	}

}
