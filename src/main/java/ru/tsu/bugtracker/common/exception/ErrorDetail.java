package ru.tsu.bugtracker.common.exception;

/**
 * Describes a single field-level validation or request error.
 *
 * @param field   name of the offending field, or {@code null} for non-field errors
 * @param message human-readable description of the problem
 */
public record ErrorDetail(String field, String message) {
}
