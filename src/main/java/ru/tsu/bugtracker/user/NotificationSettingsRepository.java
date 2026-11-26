package ru.tsu.bugtracker.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {

	Optional<NotificationSettings> findByUserId(UUID userId);

}
