package ru.tsu.bugtracker.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.admin.dto.AdminRoleRequest;
import ru.tsu.bugtracker.admin.dto.AdminUserDetailsDto;
import ru.tsu.bugtracker.admin.dto.AdminUserSummaryDto;
import ru.tsu.bugtracker.admin.dto.BlockUserRequest;
import ru.tsu.bugtracker.admin.dto.DeactivateUserRequest;
import ru.tsu.bugtracker.audit.AuditLogEntryDto;
import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.security.AuthenticatedUser;
import ru.tsu.bugtracker.user.UserStatus;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Administrative user management and audit log endpoints")
public class AdminController {

	private final AdminService adminService;

	private final AuditService auditService;

	public AdminController(AdminService adminService, AuditService auditService) {
		this.adminService = adminService;
		this.auditService = auditService;
	}

	@GetMapping("/users")
	@Operation(summary = "List users", description = "Returns a paged list of users with optional search and filters")
	public PagedResponse<AdminUserSummaryDto> listUsers(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) UserStatus status,
			@RequestParam(required = false) Boolean isAdmin,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return adminService.listUsers(search, status, isAdmin, PageRequest.of(page, size));
	}

	@GetMapping("/users/{userId}")
	@Operation(summary = "Get user details", description = "Returns the profile of the specified user")
	public AdminUserDetailsDto getUser(@PathVariable UUID userId) {
		return adminService.getUser(userId);
	}

	@PostMapping("/users/{userId}/block")
	@Operation(summary = "Block user", description = "Blocks the specified user and revokes active sessions")
	public AdminUserDetailsDto blockUser(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID userId,
			@RequestBody(required = false) @Valid BlockUserRequest request) {
		return adminService.blockUser(currentUser.id(), userId, request);
	}

	@PostMapping("/users/{userId}/unblock")
	@Operation(summary = "Unblock user", description = "Restores a blocked user to the active status")
	public AdminUserDetailsDto unblockUser(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID userId) {
		return adminService.unblockUser(currentUser.id(), userId);
	}

	@PostMapping("/users/{userId}/password-reset")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(summary = "Trigger password reset", description = "Sends a password reset link to the specified user")
	public MessageResponse triggerPasswordReset(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID userId) {
		return adminService.triggerPasswordReset(currentUser.id(), userId);
	}

	@PostMapping("/users/{userId}/verify-email")
	@Operation(summary = "Verify user e-mail", description = "Marks the e-mail of the specified user as verified")
	public AdminUserDetailsDto verifyUserEmail(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID userId) {
		return adminService.verifyUserEmail(currentUser.id(), userId);
	}

	@PatchMapping("/users/{userId}/admin-role")
	@Operation(summary = "Update admin role", description = "Grants or revokes the global administrator role")
	public AdminUserDetailsDto updateAdminRole(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID userId,
			@Valid @RequestBody AdminRoleRequest request) {
		return adminService.updateAdminRole(currentUser.id(), userId, request);
	}

	@PostMapping("/users/{userId}/deactivate")
	@Operation(summary = "Deactivate user", description = "Deactivates the specified user and revokes active sessions")
	public AdminUserDetailsDto deactivateUser(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@PathVariable UUID userId,
			@RequestBody(required = false) @Valid DeactivateUserRequest request) {
		return adminService.deactivateUser(currentUser.id(), userId, request);
	}

	@GetMapping("/audit-logs")
	@Operation(summary = "List audit logs", description = "Returns a paged list of administrative actions with optional filters")
	public PagedResponse<AuditLogEntryDto> listAuditLogs(
			@RequestParam(required = false) UUID actorId,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) String targetType,
			@RequestParam(required = false) UUID targetId,
			@RequestParam(required = false) OffsetDateTime from,
			@RequestParam(required = false) OffsetDateTime to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return auditService.listAuditLogs(actorId, action, targetType, targetId, from, to,
				PageRequest.of(page, size));
	}

}
