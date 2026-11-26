package ru.tsu.bugtracker.admin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.tsu.bugtracker.admin.dto.AdminRoleRequest;
import ru.tsu.bugtracker.admin.dto.BlockUserRequest;
import ru.tsu.bugtracker.admin.dto.DeactivateUserRequest;
import ru.tsu.bugtracker.audit.AuditService;
import ru.tsu.bugtracker.auth.PasswordResetTokenRepository;
import ru.tsu.bugtracker.auth.RefreshTokenRepository;
import ru.tsu.bugtracker.common.exception.ConflictException;
import ru.tsu.bugtracker.mail.MailService;
import ru.tsu.bugtracker.project.ProjectInvitationRepository;
import ru.tsu.bugtracker.project.ProjectMemberRepository;
import ru.tsu.bugtracker.user.User;
import ru.tsu.bugtracker.user.UserRepository;
import ru.tsu.bugtracker.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private ProjectInvitationRepository projectInvitationRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	private AuditService auditService;

	@Mock
	private MailService mailService;

	@InjectMocks
	private AdminService adminService;

	@Test
	void blockUserSelfThrowsConflict() {
		UUID actorId = UUID.randomUUID();
		User target = new User();
		target.setId(actorId);
		target.setStatus(UserStatus.ACTIVE);
		when(userRepository.findById(actorId)).thenReturn(Optional.of(target));

		assertThrows(ConflictException.class,
				() -> adminService.blockUser(actorId, actorId, new BlockUserRequest("self block")));
	}

	@Test
	void updateAdminRoleLastAdminThrowsConflict() {
		UUID actorId = UUID.randomUUID();
		UUID targetUserId = UUID.randomUUID();
		User target = new User();
		target.setId(targetUserId);
		target.setAdmin(true);
		target.setStatus(UserStatus.ACTIVE);
		when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));
		when(userRepository.countByAdminTrueAndStatusNot(UserStatus.DEACTIVATED)).thenReturn(1L);

		assertThrows(ConflictException.class,
				() -> adminService.updateAdminRole(actorId, targetUserId, new AdminRoleRequest(false)));
	}

	@Test
	void deactivateSelfThrowsConflict() {
		UUID actorId = UUID.randomUUID();
		User target = new User();
		target.setId(actorId);
		target.setStatus(UserStatus.ACTIVE);
		when(userRepository.findById(actorId)).thenReturn(Optional.of(target));

		assertThrows(ConflictException.class,
				() -> adminService.deactivateUser(actorId, actorId, new DeactivateUserRequest("self deactivate")));
	}

}
