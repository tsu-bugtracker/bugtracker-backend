package ru.tsu.bugtracker.user.dto;

/**
 * Delivery channels enabled for a notification event.
 *
 * @param inApp whether in-app notifications are enabled
 * @param email whether email notifications are enabled
 */
public record NotificationChannelConfigDto(boolean inApp, boolean email) {
}
