package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist.
 */
public class NotFoundException extends ApiException {

	public static final String CODE = "NOT_FOUND";

	public NotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, CODE, message);
	}

	public NotFoundException(String message, List<ErrorDetail> details) {
		super(HttpStatus.NOT_FOUND, CODE, message, details);
	}

	public NotFoundException(String message, Throwable cause) {
		super(HttpStatus.NOT_FOUND, CODE, message, cause);
	}

}
