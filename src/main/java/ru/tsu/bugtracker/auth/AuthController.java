package ru.tsu.bugtracker.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.tsu.bugtracker.auth.dto.AuthTokensResponse;
import ru.tsu.bugtracker.auth.dto.LoginRequest;
import ru.tsu.bugtracker.auth.dto.LogoutRequest;
import ru.tsu.bugtracker.auth.dto.PasswordResetConfirmRequest;
import ru.tsu.bugtracker.auth.dto.PasswordResetRequest;
import ru.tsu.bugtracker.auth.dto.RefreshTokenRequest;
import ru.tsu.bugtracker.auth.dto.RegisterRequest;
import ru.tsu.bugtracker.auth.dto.ResendVerificationRequest;
import ru.tsu.bugtracker.auth.dto.VerifyEmailRequest;
import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.security.AuthenticatedUser;

/**
 * Public authentication endpoints: registration, e-mail verification, login,
 * token refresh, logout and password recovery.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, tokens and password recovery")
public class AuthController {

	private final AuthService authService;

	private final AuthTokenService authTokenService;

	public AuthController(AuthService authService, AuthTokenService authTokenService) {
		this.authService = authService;
		this.authTokenService = authTokenService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a user", description = "Creates a guest account and sends an e-mail verification letter")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/verify-email")
	@Operation(summary = "Verify e-mail", description = "Confirms the e-mail address using a one-time token")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		return authService.verifyEmail(request);
	}

	@PostMapping("/resend-verification")
	@Operation(summary = "Resend verification e-mail", description = "Sends the e-mail verification letter again")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
		return authService.resendVerification(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Log in", description = "Checks credentials and issues an access/refresh token pair")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new token pair")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return authTokenService.refresh(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Log out", description = "Ends the current session and revokes the refresh token when supplied")
	public MessageResponse logout(
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			@RequestBody(required = false) LogoutRequest request) {
		return authTokenService.logout(currentUser.id(), request);
	}

	@PostMapping("/password-reset/request")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(summary = "Request password reset", description = "Sends a password reset link without revealing whether the account exists")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public MessageResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		return authTokenService.requestPasswordReset(request);
	}

	@PostMapping("/password-reset/confirm")
	@Operation(summary = "Confirm password reset", description = "Sets a new password using a one-time reset token")
	@io.swagger.v3.oas.annotations.security.SecurityRequirements
	public MessageResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
		return authTokenService.confirmPasswordReset(request);
	}

}
