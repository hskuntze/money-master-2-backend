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

    public AccessLogService(AccessLogRepository repository, RequestMetadataExtractor metadataExtractor) {
        this.repository = repository;
        this.metadataExtractor = metadataExtractor;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        try {
            AccessLog accessLog = new AccessLog();
            accessLog.setOccurredAt(Instant.now());
            accessLog.setMethod(request.getMethod());
            accessLog.setPath(request.getRequestURI());
            accessLog.setQueryString(request.getQueryString());
            accessLog.setStatusCode(response.getStatus());
            accessLog.setDurationMs(durationMs);
            accessLog.setPrincipal(metadataExtractor.principal(request));
            accessLog.setClientIp(metadataExtractor.clientIp(request));
            accessLog.setUserAgent(metadataExtractor.userAgent(request));
            accessLog.setSuccess(response.getStatus() < 400);
            repository.save(accessLog);
        } catch (Exception ex) {
            log.warn("Não foi possível registrar log de acesso: {}", ex.getMessage(), ex);
        }
    }
}
