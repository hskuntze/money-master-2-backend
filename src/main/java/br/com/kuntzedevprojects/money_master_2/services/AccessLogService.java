package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.entities.AccessLog;
import br.com.kuntzedevprojects.money_master_2.repositories.AccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AccessLogService {

    private static final Logger log = LoggerFactory.getLogger(AccessLogService.class);

    private final AccessLogRepository repository;
    private final RequestMetadataExtractor metadataExtractor;
    private final SecurityAuditService securityAuditService;

    public AccessLogService(
            AccessLogRepository repository,
            RequestMetadataExtractor metadataExtractor,
            SecurityAuditService securityAuditService
    ) {
        this.repository = repository;
        this.metadataExtractor = metadataExtractor;
        this.securityAuditService = securityAuditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        try {
            AccessLog accessLog = new AccessLog();
            accessLog.setOccurredAt(Instant.now());
            accessLog.setMethod(request.getMethod());
            accessLog.setPath(request.getRequestURI());
            accessLog.setQueryString(metadataExtractor.safeQueryString(request));
            accessLog.setStatusCode(response.getStatus());
            accessLog.setDurationMs(durationMs);
            accessLog.setPrincipal(metadataExtractor.principal(request));
            accessLog.setClientIp(metadataExtractor.clientIp(request));
            accessLog.setUserAgent(metadataExtractor.userAgent(request));
            accessLog.setSuccess(response.getStatus() < 400);
            repository.save(accessLog);
            recordSensitiveWriteIfNeeded(request, response);
        } catch (Exception ex) {
            log.warn("Não foi possível registrar log de acesso: {}", ex.getMessage(), ex);
        }
    }

    private void recordSensitiveWriteIfNeeded(HttpServletRequest request, HttpServletResponse response) {
        if (response.getStatus() >= 400 || !isWriteMethod(request.getMethod())) {
            return;
        }
        String path = request.getRequestURI();
        if (path == null || path.contains("/auth/")) {
            return;
        }
        securityAuditService.record("SENSITIVE_WRITE", null, true, request,
                "Operação de escrita executada com sucesso. Payload não registrado por segurança.");
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }
}
