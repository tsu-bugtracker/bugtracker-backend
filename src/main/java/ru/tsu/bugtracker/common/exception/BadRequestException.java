package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is malformed or contains invalid data.
 */
public class BadRequestException extends ApiException {

	public static final String CODE = "BAD_REQUEST";

	public BadRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, CODE, message);
	}

	public BadRequestException(String message, List<ErrorDetail> details) {
		super(HttpStatus.BAD_REQUEST, CODE, message, details);
	}

	public BadRequestException(String message, Throwable cause) {
		super(HttpStatus.BAD_REQUEST, CODE, message, cause);
	}

}
