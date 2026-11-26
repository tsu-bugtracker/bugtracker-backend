package ru.tsu.bugtracker.auth;

import ru.tsu.bugtracker.auth.dto.*;
import ru.tsu.bugtracker.user.*;
import ru.tsu.bugtracker.user.dto.UserProfileDto;
import ru.tsu.bugtracker.common.dto.MessageResponse;
import ru.tsu.bugtracker.common.util.TokenHashUtil;
import ru.tsu.bugtracker.common.exception.*;
import ru.tsu.bugtracker.security.*;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.audit.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthTokenService {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;

    public AuthTokenService(JwtService jwtService,
                            JwtProperties jwtProperties,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            MailService mailService,
                            AuditService auditService) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.auditService = auditService;
    }

    public AuthTokensResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.isAdmin());
        String rawRefresh = TokenHashUtil.generateToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHashUtil.sha256Hex(rawRefresh));
        refreshToken.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC)
                .plusNanos(jwtProperties.getRefreshExpirationMs() * 1_000_000L));
        refreshToken.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        refreshTokenRepository.save(refreshToken);

        return new AuthTokensResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtProperties.getAccessExpirationMs() / 1000L,
                UserProfileDto.from(user)
        );
    }

    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String tokenHash = TokenHashUtil.sha256Hex(request.refreshToken().trim());

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token is revoked or expired");
        }

        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE || !user.isEmailVerified()) {
            throw new ForbiddenException("User account is not active or email is not verified");
        }

        token.setRevokedAt(now);
        refreshTokenRepository.save(token);

        return issueTokens(user);
    }

    public MessageResponse logout(UUID userId, LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = TokenHashUtil.sha256Hex(request.refreshToken().trim());
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .filter(token -> token.getUser() != null
                            && userId.equals(token.getUser().getId())
                            && token.getRevokedAt() == null)
                    .ifPresent(token -> {
                        token.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        refreshTokenRepository.save(token);
                    });
        } else {
            revokeAll(userId);
        }

        return new MessageResponse("Logged out successfully");
    }

    public void revokeAll(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.setRevokedAt(now));
    }

    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        userOpt.filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                    String rawToken = TokenHashUtil.generateToken();

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setTokenHash(TokenHashUtil.sha256Hex(rawToken));
                    token.setExpiresAt(now.plusHours(1));
                    token.setCreatedAt(now);
                    passwordResetTokenRepository.save(token);

                    mailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
                });

        return new MessageResponse("If an account with that email exists, a password reset link has been sent");
    }

    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String tokenHash = TokenHashUtil.sha256Hex(request.token().trim());

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnprocessableEntityException("Invalid or expired password reset token"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new UnprocessableEntityException("Invalid or expired password reset token");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);

        revokeAll(user.getId());

        auditService.log(user, "PASSWORD_RESET", "USER", user.getId(), Map.of());

        return new MessageResponse("Password has been reset successfully");
    }
}
