package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication is missing or invalid.
 */
public class UnauthorizedException extends ApiException {

	public static final String CODE = "UNAUTHORIZED";

	public UnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED, CODE, message);
	}

	public UnauthorizedException(String message, List<ErrorDetail> details) {
		super(HttpStatus.UNAUTHORIZED, CODE, message, details);
	}

	public UnauthorizedException(String message, Throwable cause) {
		super(HttpStatus.UNAUTHORIZED, CODE, message, cause);
	}

}
