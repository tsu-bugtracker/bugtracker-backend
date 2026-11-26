package ru.tsu.bugtracker.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {

	Optional<ProjectInvitation> findByTokenHash(String tokenHash);

	List<ProjectInvitation> findAllByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

}
