package ru.tsu.bugtracker.user.dto;

/**
 * Notification settings of the current user.
 *
 * @param inAppEnabled whether in-app notifications are enabled globally
 * @param emailEnabled whether email notifications are enabled globally
 * @param events       per-event channel configuration
 */
public record NotificationSettingsDto(
		boolean inAppEnabled,
		boolean emailEnabled,
		NotificationEventsConfigDto events
) {
}
