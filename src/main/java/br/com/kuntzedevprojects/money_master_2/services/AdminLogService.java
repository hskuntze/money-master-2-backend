package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.admin.AccessLogResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.FailureLogDetailResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.FailureLogResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.SecurityAuditEventResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AccessLog;
import br.com.kuntzedevprojects.money_master_2.entities.FailureLog;
import br.com.kuntzedevprojects.money_master_2.entities.SecurityAuditEvent;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.AccessLogRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FailureLogRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.SecurityAuditEventRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class AdminLogService {

    private final AccessLogRepository accessLogRepository;
    private final FailureLogRepository failureLogRepository;
    private final SecurityAuditEventRepository securityAuditEventRepository;

    public AdminLogService(
            AccessLogRepository accessLogRepository,
            FailureLogRepository failureLogRepository,
            SecurityAuditEventRepository securityAuditEventRepository
    ) {
        this.accessLogRepository = accessLogRepository;
        this.failureLogRepository = failureLogRepository;
        this.securityAuditEventRepository = securityAuditEventRepository;
    }

    @Transactional(readOnly = true)
    public Page<AccessLogResponse> accessLogs(Instant from, Instant to, String path, Integer statusCode, String principal, Pageable pageable) {
        return accessLogRepository.findAll(accessSpecification(from, to, path, statusCode, principal), pageable)
                .map(AccessLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FailureLogResponse> failureLogs(Instant from, Instant to, String path, Integer statusCode, String principal, Pageable pageable) {
        return failureLogRepository.findAll(failureSpecification(from, to, path, statusCode, principal), pageable)
                .map(FailureLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SecurityAuditEventResponse> securityEvents(
            Instant from,
            Instant to,
            String eventType,
            String path,
            String principal,
            String clientIp,
            Boolean success,
            Pageable pageable
    ) {
        return securityAuditEventRepository.findAll(securityEventSpecification(from, to, eventType, path, principal, clientIp, success), pageable)
                .map(SecurityAuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public FailureLogDetailResponse failureLog(Long id) {
        FailureLog log = failureLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log de falha não encontrado."));
        return FailureLogDetailResponse.from(log);
    }

    private Specification<AccessLog> accessSpecification(Instant from, Instant to, String path, Integer statusCode, String principal) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            if (path != null && !path.isBlank()) predicates.add(cb.like(cb.lower(root.get("path")), "%" + path.toLowerCase() + "%"));
            if (statusCode != null) predicates.add(cb.equal(root.get("statusCode"), statusCode));
            if (principal != null && !principal.isBlank()) predicates.add(cb.like(cb.lower(root.get("principal")), "%" + principal.toLowerCase() + "%"));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<FailureLog> failureSpecification(Instant from, Instant to, String path, Integer statusCode, String principal) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            if (path != null && !path.isBlank()) predicates.add(cb.like(cb.lower(root.get("path")), "%" + path.toLowerCase() + "%"));
            if (statusCode != null) predicates.add(cb.equal(root.get("statusCode"), statusCode));
            if (principal != null && !principal.isBlank()) predicates.add(cb.like(cb.lower(root.get("principal")), "%" + principal.toLowerCase() + "%"));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<SecurityAuditEvent> securityEventSpecification(
            Instant from,
            Instant to,
            String eventType,
            String path,
            String principal,
            String clientIp,
            Boolean success
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            if (eventType != null && !eventType.isBlank()) predicates.add(cb.like(cb.lower(root.get("eventType")), "%" + eventType.toLowerCase() + "%"));
            if (path != null && !path.isBlank()) predicates.add(cb.like(cb.lower(root.get("path")), "%" + path.toLowerCase() + "%"));
            if (principal != null && !principal.isBlank()) predicates.add(cb.like(cb.lower(root.get("principal")), "%" + principal.toLowerCase() + "%"));
            if (clientIp != null && !clientIp.isBlank()) predicates.add(cb.equal(root.get("clientIp"), clientIp));
            if (success != null) predicates.add(cb.equal(root.get("success"), success));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
