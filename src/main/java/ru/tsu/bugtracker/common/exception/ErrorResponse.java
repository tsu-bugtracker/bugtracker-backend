package ru.tsu.bugtracker.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard payload returned to clients when a request fails.
 *
 * @param timestamp moment the error occurred
 * @param status    HTTP status code
 * @param error     HTTP status reason phrase
 * @param code      stable application-specific error code
 * @param message   human-readable error description
 * @param path      request path that produced the error
 * @param details   optional list of field-level errors
 */
public record ErrorResponse(
		OffsetDateTime timestamp,
		int status,
		String error,
		String code,
		String message,
		String path,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<ErrorDetail> details
) {
}
