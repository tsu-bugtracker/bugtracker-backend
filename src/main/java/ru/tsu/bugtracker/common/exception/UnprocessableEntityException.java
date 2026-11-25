package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is syntactically valid but cannot be processed.
 */
public class UnprocessableEntityException extends ApiException {

	public static final String CODE = "UNPROCESSABLE_ENTITY";

	public UnprocessableEntityException(String message) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, CODE, message);
	}

	public UnprocessableEntityException(String message, List<ErrorDetail> details) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, CODE, message, details);
	}

	public UnprocessableEntityException(String message, Throwable cause) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, CODE, message, cause);
	}

}
