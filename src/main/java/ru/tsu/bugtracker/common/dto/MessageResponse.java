package ru.tsu.bugtracker.common.dto;

/**
 * Simple response carrying a human-readable message.
 *
 * @param message message text
 */
public record MessageResponse(String message) {
}
