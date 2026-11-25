package ru.tsu.bugtracker.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(
		UUID id,
		String email,
		String passwordHash,
		boolean admin,
		boolean active,
		boolean emailVerified
) implements UserDetails {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private static final String ROLE_USER = "ROLE_USER";

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(admin ? ROLE_ADMIN : ROLE_USER));
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public boolean isEnabled() {
		return active && emailVerified;
	}

	@Override
	public boolean isAccountNonLocked() {
		return active;
	}

}
