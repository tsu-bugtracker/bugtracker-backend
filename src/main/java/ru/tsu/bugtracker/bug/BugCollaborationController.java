package ru.tsu.bugtracker.bug;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ru.tsu.bugtracker.bug.dto.AttachmentDto;
import ru.tsu.bugtracker.bug.dto.BugLinkDto;
import ru.tsu.bugtracker.bug.dto.CommentDto;
import ru.tsu.bugtracker.bug.dto.CreateBugLinkRequest;
import ru.tsu.bugtracker.bug.dto.CreateCommentRequest;
import ru.tsu.bugtracker.bug.dto.UpdateCommentRequest;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Bug collaboration endpoints: comments, attachments and links between bugs.
 */
@RestController
@Tag(name = "Bugs")
public class BugCollaborationController {

	private final BugCollaborationService bugCollaborationService;

	public BugCollaborationController(BugCollaborationService bugCollaborationService) {
		this.bugCollaborationService = bugCollaborationService;
	}

	@GetMapping("/api/v1/bugs/{bugId}/comments")
	@Operation(summary = "List bug comments")
	public PagedResponse<CommentDto> listComments(@PathVariable UUID bugId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.bugCollaborationService.listComments(bugId, currentUser.id(),
				PageRequest.of(page, size));
	}

	@PostMapping("/api/v1/bugs/{bugId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create bug comment")
	public CommentDto createComment(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody CreateCommentRequest request) {
		return this.bugCollaborationService.createComment(bugId, currentUser.id(), request);
	}

	@PatchMapping("/api/v1/bugs/{bugId}/comments/{commentId}")
	@Operation(summary = "Update bug comment")
	public CommentDto updateComment(@PathVariable UUID bugId, @PathVariable UUID commentId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody UpdateCommentRequest request) {
		return this.bugCollaborationService.updateComment(bugId, commentId, currentUser.id(), request);
	}

	@DeleteMapping("/api/v1/bugs/{bugId}/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete bug comment")
	public void deleteComment(@PathVariable UUID bugId, @PathVariable UUID commentId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		this.bugCollaborationService.deleteComment(bugId, commentId, currentUser.id());
	}

	@GetMapping("/api/v1/bugs/{bugId}/attachments")
	@Operation(summary = "List bug attachments")
	public List<AttachmentDto> listAttachments(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.bugCollaborationService.listAttachments(bugId, currentUser.id());
	}

	@PostMapping(value = "/api/v1/bugs/{bugId}/attachments",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Upload bug attachment")
	public AttachmentDto uploadAttachment(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "commentId", required = false) UUID commentId) {
		return this.bugCollaborationService.uploadAttachment(bugId, currentUser.id(), file, commentId);
	}

	@GetMapping("/api/v1/attachments/{attachmentId}/download")
	@Operation(summary = "Download attachment")
	public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID attachmentId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		BugCollaborationService.AttachmentDownload download =
				this.bugCollaborationService.downloadAttachment(attachmentId, currentUser.id());
		ContentDisposition contentDisposition = ContentDisposition.attachment()
			.filename(download.fileName(), StandardCharsets.UTF_8)
			.build();
		return ResponseEntity.ok()
			.contentType(resolveContentType(download.contentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
			.body(download.data());
	}

	@DeleteMapping("/api/v1/attachments/{attachmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete attachment")
	public void deleteAttachment(@PathVariable UUID attachmentId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		this.bugCollaborationService.deleteAttachment(attachmentId, currentUser.id());
	}

	@PostMapping("/api/v1/bugs/{bugId}/links")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Link bugs")
	public BugLinkDto linkBugs(@PathVariable UUID bugId,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody CreateBugLinkRequest request) {
		return this.bugCollaborationService.linkBugs(bugId, currentUser.id(), request);
	}

	@DeleteMapping("/api/v1/bugs/{bugId}/links/{linkId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete bug link")
	public void unlinkBugs(@PathVariable UUID bugId, @PathVariable UUID linkId,
			@AuthenticationPrincipal AuthenticatedUser currentUser) {
		this.bugCollaborationService.unlinkBugs(bugId, linkId, currentUser.id());
	}

	private static MediaType resolveContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(contentType);
		}
		catch (IllegalArgumentException ex) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

}
