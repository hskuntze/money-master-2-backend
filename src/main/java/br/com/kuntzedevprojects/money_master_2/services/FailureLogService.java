package br.com.kuntzedevprojects.money_master_2.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.entities.FailureLog;
import br.com.kuntzedevprojects.money_master_2.repositories.FailureLogRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class FailureLogService {

    private static final Logger log = LoggerFactory.getLogger(FailureLogService.class);

    private final FailureLogRepository repository;
    private final RequestMetadataExtractor metadataExtractor;

    public FailureLogService(FailureLogRepository repository, RequestMetadataExtractor metadataExtractor) {
        this.repository = repository;
        this.metadataExtractor = metadataExtractor;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Exception exception, HttpStatus status, HttpServletRequest request, String userMessage) {
        try {
            FailureLog failureLog = new FailureLog();
            failureLog.setOccurredAt(Instant.now());
            failureLog.setMethod(request.getMethod());
            failureLog.setPath(request.getRequestURI());
            failureLog.setQueryString(metadataExtractor.safeQueryString(request));
            failureLog.setStatusCode(status.value());
            failureLog.setPrincipal(metadataExtractor.principal(request));
            failureLog.setClientIp(metadataExtractor.clientIp(request));
            failureLog.setUserAgent(metadataExtractor.userAgent(request));
            failureLog.setExceptionClass(exception.getClass().getName());
            failureLog.setMessage(resolveMessage(exception, userMessage));
            failureLog.setStackTrace(stackTrace(exception));
            repository.save(failureLog);
        } catch (Exception logException) {
            log.warn("Não foi possível registrar log de falha: {}", logException.getMessage(), logException);
        }
    }

    private String resolveMessage(Exception exception, String userMessage) {
        if (userMessage != null && !userMessage.isBlank()) {
            return userMessage;
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName();
    }

    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
