package ru.tsu.bugtracker.admin.dto;

import java.util.UUID;

import ru.tsu.bugtracker.project.ProjectMember;
import ru.tsu.bugtracker.project.ProjectRole;
import ru.tsu.bugtracker.project.ProjectStatus;

public record UserProjectMembershipDto(
		UUID projectId,
		String projectName,
		ProjectRole role,
		ProjectStatus projectStatus) {

	public static UserProjectMembershipDto from(ProjectMember member) {
		return new UserProjectMembershipDto(
				member.getProject().getId(),
				member.getProject().getName(),
				member.getRole(),
				member.getProject().getStatus());
	}

}
