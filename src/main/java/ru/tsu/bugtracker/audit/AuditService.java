package ru.tsu.bugtracker.audit;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.tsu.bugtracker.common.dto.PagedResponse;
import ru.tsu.bugtracker.common.dto.UserShortDto;
import ru.tsu.bugtracker.user.User;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {
	};

	private final AuditLogRepository auditLogRepository;

	private final ObjectMapper objectMapper;

	public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
		this.auditLogRepository = auditLogRepository;
		this.objectMapper = objectMapper;
	}

	/**
	 * Records an audit log entry.
	 *
	 * @param actor      user who performed the action, or {@code null} for system actions
	 * @param action     action name
	 * @param targetType type of the affected entity
	 * @param targetId   identifier of the affected entity, or {@code null}
	 * @param details    free-form action details, or {@code null}
	 */
	@Transactional
	public void log(User actor, String action, String targetType, UUID targetId, Map<String, Object> details) {
		AuditLog entry = new AuditLog();
		entry.setActor(actor);
		entry.setAction(action);
		entry.setTargetType(targetType);
		entry.setTargetId(targetId);
		entry.setDetailsJson(serializeDetails(action, targetType, details));
		auditLogRepository.save(entry);
	}

	/**
	 * Returns a filtered page of audit log entries ordered by creation timestamp, newest first.
	 *
	 * @param actorId    filter by actor identifier, or {@code null}
	 * @param action     filter by action name, or {@code null}
	 * @param targetType filter by target type, or {@code null}
	 * @param targetId   filter by target identifier, or {@code null}
	 * @param from       lower bound for the creation timestamp (inclusive), or {@code null}
	 * @param to         upper bound for the creation timestamp (inclusive), or {@code null}
	 * @param pageable   pagination
	 * @return page of audit log entries
	 */
	@Transactional(readOnly = true)
	public PagedResponse<AuditLogEntryDto> listAuditLogs(UUID actorId, String action, String targetType,
			UUID targetId, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
		Specification<AuditLog> specification = (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (actorId != null) {
				predicates.add(criteriaBuilder.equal(root.get("actor").get("id"), actorId));
			}
			if (action != null && !action.isBlank()) {
				predicates.add(criteriaBuilder.equal(root.get("action"), action));
			}
			if (targetType != null && !targetType.isBlank()) {
				predicates.add(criteriaBuilder.equal(root.get("targetType"), targetType));
			}
			if (targetId != null) {
				predicates.add(criteriaBuilder.equal(root.get("targetId"), targetId));
			}
			if (from != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
			}
			if (to != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};

		Pageable sortedPageable = pageable.isPaged()
				? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
						Sort.by(Sort.Direction.DESC, "createdAt"))
				: pageable;

		Page<AuditLogEntryDto> page = auditLogRepository.findAll(specification, sortedPageable)
				.map(this::toDto);
		return PagedResponse.from(page);
	}

	private String serializeDetails(String action, String targetType, Map<String, Object> details) {
		if (details == null || details.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(details);
		}
		catch (JacksonException e) {
			log.warn("Failed to serialize details of audit action {} on {}: {}", action, targetType, e.getMessage());
			return null;
		}
	}

	private AuditLogEntryDto toDto(AuditLog entry) {
		return new AuditLogEntryDto(
				entry.getId(),
				entry.getActor() == null ? null : UserShortDto.from(entry.getActor()),
				entry.getAction(),
				entry.getTargetType(),
				entry.getTargetId(),
				deserializeDetails(entry),
				entry.getCreatedAt()
		);
	}

	private Map<String, Object> deserializeDetails(AuditLog entry) {
		String detailsJson = entry.getDetailsJson();
		if (detailsJson == null || detailsJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(detailsJson, DETAILS_TYPE);
		}
		catch (JacksonException e) {
			log.warn("Failed to deserialize details of audit log entry {}: {}", entry.getId(), e.getMessage());
			return null;
		}
	}

}
