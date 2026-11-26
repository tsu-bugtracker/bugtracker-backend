package ru.tsu.bugtracker.bug.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugLink;
import ru.tsu.bugtracker.bug.BugLinkType;
import ru.tsu.bugtracker.bug.BugStatus;

/**
 * Representation of a link between two bugs.
 *
 * @param id              link identifier
 * @param linkType        type of the link
 * @param targetBugId     identifier of the linked bug
 * @param targetBugKey    human-readable key of the linked bug
 * @param targetBugTitle  title of the linked bug
 * @param targetBugStatus status of the linked bug
 * @param createdAt       creation timestamp
 */
public record BugLinkDto(
		UUID id,
		BugLinkType linkType,
		UUID targetBugId,
		String targetBugKey,
		String targetBugTitle,
		BugStatus targetBugStatus,
		OffsetDateTime createdAt
) {

	public static BugLinkDto from(BugLink link) {
		Bug target = link.getTargetBug();
		return new BugLinkDto(
				link.getId(),
				link.getLinkType(),
				target != null ? target.getId() : null,
				target != null ? target.getBugKey() : null,
				target != null ? target.getTitle() : null,
				target != null ? target.getStatus() : null,
				link.getCreatedAt()
		);
	}

}
