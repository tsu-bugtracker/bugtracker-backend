package ru.tsu.bugtracker.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenHashUtil {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final int TOKEN_BYTES = 32;

	private TokenHashUtil() {
	}

	public static String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String generateRawToken() {
		return generateToken();
	}

	public static String sha256Hex(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm is not available", ex);
		}
	}

}
