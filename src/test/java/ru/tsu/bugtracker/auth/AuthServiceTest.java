package ru.tsu.bugtracker.auth;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.auth.dto.LoginRequest;
import ru.tsu.bugtracker.auth.dto.RegisterRequest;
import ru.tsu.bugtracker.auth.dto.VerifyEmailRequest;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.common.exception.UnprocessableEntityException;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.project.ProjectInvitationRepository;
import ru.tsu.bugtracker.project.ProjectMemberRepository;
import ru.tsu.bugtracker.user.NotificationSettingsRepository;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthTokenService authTokenService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationSettingsRepository notificationSettingsRepository;

	@Mock
	private EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private ProjectInvitationRepository projectInvitationRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private MailService mailService;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private AuthService authService;

	@Test
	void registerThrowsConflictWhenEmailExists() {
		RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User", null);
		when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

		assertThrows(ConflictException.class, () -> authService.register(request));
	}

	@Test
	void loginThrowsForbiddenWhenEmailNotVerified() {
		User user = new User();
		user.setEmail("user@example.com");
		user.setPasswordHash("hashed-password");
		user.setStatus(UserStatus.ACTIVE);
		user.setEmailVerified(false);

		when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

		LoginRequest request = new LoginRequest("user@example.com", "password123");

		assertThrows(ForbiddenException.class, () -> authService.login(request));
	}

	@Test
	void verifyEmailThrowsUnprocessableWhenExpired() {
		EmailVerificationToken token = new EmailVerificationToken();
		token.setExpiresAt(OffsetDateTime.now().minusHours(1));

		when(emailVerificationTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

		VerifyEmailRequest request = new VerifyEmailRequest("expired-token");

		assertThrows(UnprocessableEntityException.class, () -> authService.verifyEmail(request));
	}

}
