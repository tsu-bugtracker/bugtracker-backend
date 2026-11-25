package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user lacks permission to perform an action.
 */
public class ForbiddenException extends ApiException {

	public static final String CODE = "FORBIDDEN";

	public ForbiddenException(String message) {
		super(HttpStatus.FORBIDDEN, CODE, message);
	}

	public ForbiddenException(String message, List<ErrorDetail> details) {
		super(HttpStatus.FORBIDDEN, CODE, message, details);
	}

	public ForbiddenException(String message, Throwable cause) {
		super(HttpStatus.FORBIDDEN, CODE, message, cause);
	}

}
