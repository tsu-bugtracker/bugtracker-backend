package ru.tsu.bugtracker.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.security.AuthenticatedUser;
import ru.tsu.bugtracker.user.dto.ChangePasswordRequest;
import ru.tsu.bugtracker.user.dto.NotificationSettingsDto;
import ru.tsu.bugtracker.user.dto.UpdateProfileRequest;
import ru.tsu.bugtracker.user.dto.UserProfileDto;

/**
 * Profile endpoints for the currently authenticated user.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Profile")
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping
	@Operation(summary = "Get current user profile")
	public UserProfileDto getCurrentProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.userProfileService.getCurrentProfile(currentUser.id());
	}

	@PatchMapping
	@Operation(summary = "Update current user profile")
	public UserProfileDto updateCurrentProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody UpdateProfileRequest request) {
		return this.userProfileService.updateCurrentProfile(currentUser.id(), request);
	}

	@PutMapping("/password")
	@Operation(summary = "Change current user password")
	public MessageResponse changeCurrentPassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody ChangePasswordRequest request) {
		return this.userProfileService.changeCurrentPassword(currentUser.id(), request);
	}

	@GetMapping("/notification-settings")
	@Operation(summary = "Get current user notification settings")
	public NotificationSettingsDto getNotificationSettings(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return this.userProfileService.getNotificationSettings(currentUser.id());
	}

	@PutMapping("/notification-settings")
	@Operation(summary = "Replace current user notification settings")
	public NotificationSettingsDto updateNotificationSettings(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@Valid @RequestBody NotificationSettingsDto request) {
		return this.userProfileService.updateNotificationSettings(currentUser.id(), request);
	}

}
