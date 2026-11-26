package ru.tsu.bugtracker.user;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.auth.RefreshToken;
import ru.tsu.bugtracker.auth.RefreshTokenRepository;
import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.common.exception.BadRequestException;
import ru.tsu.bugtracker.common.exception.NotFoundException;
import ru.tsu.bugtracker.user.dto.ChangePasswordRequest;
import ru.tsu.bugtracker.user.dto.NotificationEventsConfigDto;
import ru.tsu.bugtracker.user.dto.NotificationSettingsDto;
import ru.tsu.bugtracker.user.dto.UpdateProfileRequest;
import ru.tsu.bugtracker.user.dto.UserProfileDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Implements profile and notification settings operations for the current user.
 */
@Service
@Transactional
public class UserProfileService {

	private static final String USER_NOT_FOUND = "User not found";

	private static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";

	private static final String PASSWORD_CHANGED = "Password changed successfully";

	private static final String PASSWORD_CHANGED_ACTION = "PASSWORD_CHANGED";

	private static final String USER_TARGET_TYPE = "USER";

	private static final String EMPTY_EVENTS_JSON = "{}";

	private final UserRepository userRepository;

	private final NotificationSettingsRepository notificationSettingsRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuditService auditService;

	private final ObjectMapper objectMapper;

	public UserProfileService(UserRepository userRepository,
			NotificationSettingsRepository notificationSettingsRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			AuditService auditService,
			ObjectMapper objectMapper) {
		this.userRepository = userRepository;
		this.notificationSettingsRepository = notificationSettingsRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Returns the profile of the current user.
	 *
	 * @param userId current user identifier
	 * @return current user profile
	 */
	@Transactional(readOnly = true)
	public UserProfileDto getCurrentProfile(UUID userId) {
		return UserProfileDto.from(requireUser(userId));
	}

	/**
	 * Updates the profile of the current user. A blank full name is ignored, while the avatar
	 * URL is replaced when supplied and reset to {@code null} for a blank value.
	 *
	 * @param userId  current user identifier
	 * @param request profile changes
	 * @return updated user profile
	 */
	public UserProfileDto updateCurrentProfile(UUID userId, UpdateProfileRequest request) {
		User user = requireUser(userId);
		if (request.fullName() != null && !request.fullName().isBlank()) {
			user.setFullName(request.fullName());
		}
		if (request.avatarUrl() != null) {
			user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl());
		}
		return UserProfileDto.from(this.userRepository.save(user));
	}

	/**
	 * Changes the password of the current user, revokes all active refresh tokens and records
	 * an audit entry.
	 *
	 * @param userId  current user identifier
	 * @param request current and new passwords
	 * @return confirmation message
	 */
	public MessageResponse changeCurrentPassword(UUID userId, ChangePasswordRequest request) {
		User user = requireUser(userId);
		if (!this.passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadRequestException(CURRENT_PASSWORD_INCORRECT);
		}

		user.setPasswordHash(this.passwordEncoder.encode(request.newPassword()));
		this.userRepository.save(user);

		List<RefreshToken> activeTokens = this.refreshTokenRepository
			.findAllByUserIdAndRevokedAtIsNull(userId);
		OffsetDateTime revokedAt = OffsetDateTime.now(ZoneOffset.UTC);
		activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
		this.refreshTokenRepository.saveAll(activeTokens);

		this.auditService.log(user, PASSWORD_CHANGED_ACTION, USER_TARGET_TYPE, user.getId(), Map.of());
		return new MessageResponse(PASSWORD_CHANGED);
	}

	/**
	 * Returns notification settings of the current user, falling back to defaults when no
	 * settings have been stored yet.
	 *
	 * @param userId current user identifier
	 * @return notification settings
	 */
	@Transactional(readOnly = true)
	public NotificationSettingsDto getNotificationSettings(UUID userId) {
		requireUser(userId);
		NotificationSettings settings = this.notificationSettingsRepository.findByUserId(userId)
			.orElseGet(this::defaultSettings);
		return toDto(settings);
	}

	/**
	 * Replaces notification settings of the current user.
	 *
	 * @param userId  current user identifier
	 * @param request new notification settings
	 * @return saved notification settings
	 */
	public NotificationSettingsDto updateNotificationSettings(UUID userId, NotificationSettingsDto request) {
		User user = requireUser(userId);
		NotificationSettings settings = this.notificationSettingsRepository.findByUserId(userId)
			.orElseGet(() -> newSettings(user));

		settings.setInAppEnabled(request.inAppEnabled());
		settings.setEmailEnabled(request.emailEnabled());
		settings.setEventsJson(serializeEvents(request.events()));

		return toDto(this.notificationSettingsRepository.save(settings));
	}

	private User requireUser(UUID userId) {
		return this.userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
	}

	private NotificationSettings newSettings(User user) {
		NotificationSettings settings = new NotificationSettings();
		settings.setUser(user);
		settings.setEventsJson(EMPTY_EVENTS_JSON);
		return settings;
	}

	private NotificationSettings defaultSettings() {
		NotificationSettings settings = new NotificationSettings();
		settings.setEventsJson(EMPTY_EVENTS_JSON);
		return settings;
	}

	private NotificationSettingsDto toDto(NotificationSettings settings) {
		return new NotificationSettingsDto(
				settings.isInAppEnabled(),
				settings.isEmailEnabled(),
				deserializeEvents(settings.getEventsJson())
		);
	}

	private NotificationEventsConfigDto deserializeEvents(String eventsJson) {
		NotificationEventsConfigDto defaults = NotificationEventsConfigDto.defaults();
		if (eventsJson == null || eventsJson.isBlank()) {
			return defaults;
		}
		try {
			NotificationEventsConfigDto events = this.objectMapper
				.readValue(eventsJson, NotificationEventsConfigDto.class);
			return events != null ? withDefaults(events, defaults) : defaults;
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Failed to read notification settings", ex);
		}
	}

	private NotificationEventsConfigDto withDefaults(NotificationEventsConfigDto events,
			NotificationEventsConfigDto defaults) {
		return new NotificationEventsConfigDto(
				events.bugCreated() != null ? events.bugCreated() : defaults.bugCreated(),
				events.bugAssigned() != null ? events.bugAssigned() : defaults.bugAssigned(),
				events.bugStatusChanged() != null ? events.bugStatusChanged() : defaults.bugStatusChanged(),
				events.bugCommented() != null ? events.bugCommented() : defaults.bugCommented(),
				events.mention() != null ? events.mention() : defaults.mention(),
				events.projectInvited() != null ? events.projectInvited() : defaults.projectInvited(),
				events.releasePublished() != null ? events.releasePublished() : defaults.releasePublished()
		);
	}

	private String serializeEvents(NotificationEventsConfigDto events) {
		NotificationEventsConfigDto defaults = NotificationEventsConfigDto.defaults();
		NotificationEventsConfigDto value = events != null ? withDefaults(events, defaults) : defaults;
		try {
			return this.objectMapper.writeValueAsString(value);
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Failed to write notification settings", ex);
		}
	}

}
