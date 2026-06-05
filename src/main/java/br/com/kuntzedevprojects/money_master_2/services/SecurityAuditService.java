package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.entities.SecurityAuditEvent;
import br.com.kuntzedevprojects.money_master_2.repositories.SecurityAuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SecurityAuditEventRepository repository;
    private final RequestMetadataExtractor metadataExtractor;

    public SecurityAuditService(SecurityAuditEventRepository repository, RequestMetadataExtractor metadataExtractor) {
        this.repository = repository;
        this.metadataExtractor = metadataExtractor;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String eventType, String principal, boolean success, HttpServletRequest request, String details) {
        try {
            SecurityAuditEvent event = new SecurityAuditEvent();
            event.setOccurredAt(Instant.now());
            event.setEventType(eventType);
            event.setPrincipal(normalizePrincipal(principal != null ? principal : metadataExtractor.principal(request)));
            event.setClientIp(metadataExtractor.clientIp(request));
            event.setUserAgent(metadataExtractor.userAgent(request));
            event.setMethod(request == null ? null : request.getMethod());
            event.setPath(request == null ? null : request.getRequestURI());
            event.setSuccess(success);
            event.setDetails(details);
            repository.save(event);
        } catch (Exception ex) {
            log.warn("Não foi possível registrar evento de segurança: {}", ex.getMessage(), ex);
        }
    }

    private String normalizePrincipal(String principal) {
        return principal == null ? null : principal.trim().toLowerCase();
    }
}
