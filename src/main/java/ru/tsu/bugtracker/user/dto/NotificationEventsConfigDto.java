package ru.tsu.bugtracker.user.dto;

/**
 * Per-event notification channel configuration.
 *
 * @param bugCreated       configuration for bug creation events
 * @param bugAssigned      configuration for bug assignment events
 * @param bugStatusChanged configuration for bug status change events
 * @param bugCommented     configuration for bug comment events
 * @param mention          configuration for mention events
 * @param projectInvited   configuration for project invitation events
 * @param releasePublished configuration for release publication events
 */
public record NotificationEventsConfigDto(
		NotificationChannelConfigDto bugCreated,
		NotificationChannelConfigDto bugAssigned,
		NotificationChannelConfigDto bugStatusChanged,
		NotificationChannelConfigDto bugCommented,
		NotificationChannelConfigDto mention,
		NotificationChannelConfigDto projectInvited,
		NotificationChannelConfigDto releasePublished
) {

	public static NotificationEventsConfigDto defaults() {
		return new NotificationEventsConfigDto(
				new NotificationChannelConfigDto(true, true),
				new NotificationChannelConfigDto(true, true),
				new NotificationChannelConfigDto(true, true),
				new NotificationChannelConfigDto(true, true),
				new NotificationChannelConfigDto(true, true),
				new NotificationChannelConfigDto(true, true),
				new NotificationChannelConfigDto(true, true)
		);
	}

}
