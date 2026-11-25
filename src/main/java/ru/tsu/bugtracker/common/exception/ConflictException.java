package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request conflicts with the current state of the resource.
 */
public class ConflictException extends ApiException {

	public static final String CODE = "CONFLICT";

	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, CODE, message);
	}

	public ConflictException(String message, List<ErrorDetail> details) {
		super(HttpStatus.CONFLICT, CODE, message, details);
	}

	public ConflictException(String message, Throwable cause) {
		super(HttpStatus.CONFLICT, CODE, message, cause);
	}

}
