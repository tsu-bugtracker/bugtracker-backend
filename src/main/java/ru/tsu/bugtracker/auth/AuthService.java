package ru.tsu.bugtracker.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.auth.dto.AuthTokensResponse;
import ru.tsu.bugtracker.auth.dto.LoginRequest;
import ru.tsu.bugtracker.auth.dto.RegisterRequest;
import ru.tsu.bugtracker.auth.dto.ResendVerificationRequest;
import ru.tsu.bugtracker.auth.dto.VerifyEmailRequest;
import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.common.exception.ForbiddenException;
import ru.tsu.bugtracker.common.exception.UnauthorizedException;
import ru.tsu.bugtracker.common.exception.UnprocessableEntityException;
import ru.tsu.bugtracker.common.util.TokenHashUtil;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.project.InvitationStatus;
import ru.tsu.bugtracker.project.MemberStatus;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.ProjectInvitation;
import ru.tsu.bugtracker.project.ProjectInvitationRepository;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectMemberRepository;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.user.NotificationSettings;
import ru.tsu.bugtracker.user.NotificationSettingsRepository;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

/**
 * Registration, email verification and login flows.
 */
@Service
@Transactional
public class AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final long VERIFICATION_TOKEN_TTL_HOURS = 24L;

	private static final long RESEND_COOLDOWN_SECONDS = 60L;

	private final AuthTokenService authTokenService;

	private final UserRepository userRepository;

	private final NotificationSettingsRepository notificationSettingsRepository;

	private final EmailVerificationTokenRepository emailVerificationTokenRepository;

	private final ProjectMemberRepository projectMemberRepository;

	private final ProjectInvitationRepository projectInvitationRepository;

	private final PasswordEncoder passwordEncoder;

	private final MailService mailService;

	private final AuditService auditService;

	public AuthService(
			AuthTokenService authTokenService,
			UserRepository userRepository,
			NotificationSettingsRepository notificationSettingsRepository,
			EmailVerificationTokenRepository emailVerificationTokenRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectInvitationRepository projectInvitationRepository,
			PasswordEncoder passwordEncoder,
			MailService mailService,
			AuditService auditService) {
		this.authTokenService = authTokenService;
		this.userRepository = userRepository;
		this.notificationSettingsRepository = notificationSettingsRepository;
		this.emailVerificationTokenRepository = emailVerificationTokenRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectInvitationRepository = projectInvitationRepository;
		this.passwordEncoder = passwordEncoder;
		this.mailService = mailService;
		this.auditService = auditService;
	}

	/**
	 * Creates a new account and sends an email verification link.
	 */
	public MessageResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("A user with this email is already registered");
		}

		String invitationToken = request.invitationToken();
		if (invitationToken != null && !invitationToken.isBlank()) {
			ProjectInvitation invitation = projectInvitationRepository
					.findByTokenHash(TokenHashUtil.sha256Hex(invitationToken.trim()))
					.orElseThrow(() -> new ConflictException("Invitation not found or invalid"));
			if (invitation.getStatus() != InvitationStatus.PENDING
					|| invitation.getExpiresAt().isBefore(Instant.now())
					|| !invitation.getEmail().equalsIgnoreCase(email)) {
				throw new ConflictException("Invitation not found or invalid");
			}
		}

		User user = new User();
		user.setEmail(email);
		user.setFullName(request.fullName().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setStatus(UserStatus.ACTIVE);
		user.setEmailVerified(false);
		user.setAdmin(false);
		user.setCanCreateProjects(true);
		user = userRepository.save(user);

		NotificationSettings notificationSettings = new NotificationSettings();
		notificationSettings.setUser(user);
		notificationSettings.setInAppEnabled(true);
		notificationSettings.setEmailEnabled(true);
		notificationSettings.setEventsJson("{}");
		notificationSettingsRepository.save(notificationSettings);

		sendVerificationToken(user);

		auditService.log(user, "USER_REGISTERED", "USER", user.getId(), Map.of());

		return new MessageResponse("Registration successful. Check your inbox to verify your email.");
	}

	/**
	 * Confirms the email address and accepts pending project invitations.
	 */
	public MessageResponse verifyEmail(VerifyEmailRequest request) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		EmailVerificationToken token = emailVerificationTokenRepository
				.findByTokenHash(TokenHashUtil.sha256Hex(request.token().trim()))
				.orElseThrow(() -> new UnprocessableEntityException(
						"Verification token is invalid or has expired"));
		if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
			throw new UnprocessableEntityException("Verification token is invalid or has expired");
		}

		token.setUsedAt(now);
		User user = token.getUser();
		user.setEmailVerified(true);

		List<ProjectInvitation> invitations = projectInvitationRepository
				.findAllByEmailIgnoreCaseAndStatus(user.getEmail(), InvitationStatus.PENDING);
		for (ProjectInvitation invitation : invitations) {
			if (invitation.getExpiresAt().isBefore(Instant.now())) {
				continue;
			}
			Project project = invitation.getProject();
			if (!projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId())) {
				ProjectMember member = new ProjectMember();
				member.setProject(project);
				member.setUser(user);
				member.setRole(invitation.getRole());
				member.setStatus(MemberStatus.ACTIVE);
				projectMemberRepository.save(member);
			}
			invitation.setStatus(InvitationStatus.ACCEPTED);
			if (invitation.getRole() == ProjectRole.TESTER) {
				project.setBugTrackingEnabled(true);
			}
		}

		auditService.log(user, "EMAIL_VERIFIED", "USER", user.getId(), Map.of());

		return new MessageResponse("Email verified successfully");
	}

	/**
	 * Re-sends the verification email. The response is intentionally neutral so it does not
	 * disclose whether the account exists.
	 */
	public MessageResponse resendVerification(ResendVerificationRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
			if (user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
				return;
			}
			emailVerificationTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
					.ifPresent(lastToken -> {
						if (lastToken.getCreatedAt() != null
								&& lastToken.getCreatedAt().isAfter(now.minusSeconds(RESEND_COOLDOWN_SECONDS))) {
							throw new ConflictException(
									"A verification email can be resent at most once per minute");
						}
					});
			sendVerificationToken(user);
		});

		return new MessageResponse(
				"If an account with this email exists, a verification email will be sent.");
	}

	/**
	 * Authenticates a user and issues a new access/refresh token pair.
	 */
	public AuthTokensResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid email or password");
		}
		if (user.getStatus() != UserStatus.ACTIVE || !user.isEmailVerified()) {
			throw new ForbiddenException("Account is inactive or email is not verified");
		}
		return authTokenService.issueTokens(user);
	}

	private void sendVerificationToken(User user) {
		String rawToken = generateRawToken();
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

		EmailVerificationToken token = new EmailVerificationToken();
		token.setUser(user);
		token.setTokenHash(TokenHashUtil.sha256Hex(rawToken));
		token.setCreatedAt(now);
		token.setExpiresAt(now.plusHours(VERIFICATION_TOKEN_TTL_HOURS));
		emailVerificationTokenRepository.save(token);

		mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), rawToken);
	}

	private String generateRawToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
