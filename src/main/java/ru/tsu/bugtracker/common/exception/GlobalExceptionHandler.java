package ru.tsu.bugtracker.common.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Translates exceptions thrown while handling requests into the standard
 * {@link ErrorResponse} payload.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
		return buildResponse(ex.getStatus().value(), ex.getCode(), ex.getMessage(), request, ex.getDetails());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
			.map(this::toErrorDetail)
			.toList();
		return buildResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Validation failed", request, details);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		List<ErrorDetail> details = ex.getConstraintViolations().stream()
			.map(violation -> new ErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
		return buildResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Validation failed", request, details);
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "Bad request", request, null);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST.value(), "FILE_TOO_LARGE", "Uploaded file is too large",
			request, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "Authentication required", request, null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Access denied", request, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception while processing request {}", request.getRequestURI(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Internal server error",
			request, null);
	}

	private ErrorDetail toErrorDetail(FieldError fieldError) {
		return new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private ResponseEntity<ErrorResponse> buildResponse(int status, String code, String message,
			HttpServletRequest request, List<ErrorDetail> details) {
		ErrorResponse body = new ErrorResponse(
			OffsetDateTime.now(ZoneOffset.UTC),
			status,
			reasonPhrase(status),
			code,
			message,
			request.getRequestURI(),
			details == null || details.isEmpty() ? null : details
		);
		return ResponseEntity.status(status).body(body);
	}

	private String reasonPhrase(int status) {
		HttpStatus httpStatus = HttpStatus.resolve(status);
		return httpStatus != null ? httpStatus.getReasonPhrase() : "Error";
	}
}
