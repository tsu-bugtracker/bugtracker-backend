package ru.tsu.bugtracker.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final String CLAIM_EMAIL = "email";

	private static final String CLAIM_ADMIN = "admin";

	private static final String CLAIM_TYPE = "type";

	private static final String TOKEN_TYPE_ACCESS = "access";

	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
	}

	public String generateAccessToken(UUID userId, String email, boolean isAdmin) {
		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + this.properties.getAccessExpirationMs());

		return Jwts.builder()
			.subject(userId.toString())
			.claim(CLAIM_EMAIL, email)
			.claim(CLAIM_ADMIN, isAdmin)
			.claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
			.issuedAt(issuedAt)
			.expiration(expiration)
			.signWith(signingKey())
			.compact();
	}

	public Claims parseAndValidateAccessToken(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(signingKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();

		String type = claims.get(CLAIM_TYPE, String.class);
		if (!TOKEN_TYPE_ACCESS.equals(type)) {
			throw new JwtException("Invalid token type: expected '" + TOKEN_TYPE_ACCESS + "'");
		}

		return claims;
	}

	public UUID extractUserId(Claims claims) {
		return UUID.fromString(claims.getSubject());
	}

	public String extractEmail(Claims claims) {
		return claims.get(CLAIM_EMAIL, String.class);
	}

	public boolean extractIsAdmin(Claims claims) {
		return Boolean.TRUE.equals(claims.get(CLAIM_ADMIN, Boolean.class));
	}

	private SecretKey signingKey() {
		String secret = this.properties.getSecret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("app.jwt.secret is not configured");
		}
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

}
