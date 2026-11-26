package ru.tsu.bugtracker.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.project.MemberStatus;
import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectRole;

/**
 * Project member representation.
 *
 * @param id       membership identifier
 * @param user     member user
 * @param role     role within the project
 * @param status   membership status
 * @param joinedAt join timestamp
 */
public record ProjectMemberDto(
		UUID id,
		UserShortDto user,
		ProjectRole role,
		MemberStatus status,
		OffsetDateTime joinedAt
) {

	public static ProjectMemberDto from(ProjectMember member) {
		return new ProjectMemberDto(
				member.getId(),
				UserShortDto.from(member.getUser()),
				member.getRole(),
				member.getStatus(),
				member.getJoinedAt()
		);
	}

}
