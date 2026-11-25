package ru.tsu.bugtracker.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Base class for exceptions that are translated into an {@link ErrorResponse}.
 */
public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final List<ErrorDetail> details;

	protected ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, null, null);
	}

	protected ApiException(HttpStatus status, String code, String message, List<ErrorDetail> details) {
		this(status, code, message, details, null);
	}

	protected ApiException(HttpStatus status, String code, String message, Throwable cause) {
		this(status, code, message, null, cause);
	}

	protected ApiException(HttpStatus status, String code, String message, List<ErrorDetail> details, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.code = code;
		this.details = (details != null) ? List.copyOf(details) : null;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public List<ErrorDetail> getDetails() {
		return details;
	}

}
