package ru.tsu.bugtracker.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	Optional<User> findByEmailIgnoreCase(String email);

	default Optional<User> findByEmail(String email) {
		return findByEmailIgnoreCase(email);
	}

	boolean existsByEmailIgnoreCase(String email);

	long countByAdminTrueAndStatusNot(UserStatus status);

}
