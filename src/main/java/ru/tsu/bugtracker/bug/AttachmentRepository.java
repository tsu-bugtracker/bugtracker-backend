package ru.tsu.bugtracker.bug;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findAllByBugIdOrderByCreatedAtAsc(UUID bugId);

    List<Attachment> findAllByCommentIdOrderByCreatedAtAsc(UUID commentId);

    long countByBugId(UUID bugId);

}
