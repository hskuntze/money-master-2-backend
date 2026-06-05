package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;
import br.com.kuntzedevprojects.money_master_2.exceptions.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoginAttemptService {

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final SecurityHardeningProperties.Login properties;
    private final SecurityHardeningProperties.RateLimit rateLimitProperties;
    private final RequestMetadataExtractor metadataExtractor;
    private final SecurityAuditService securityAuditService;

    public LoginAttemptService(
            SecurityHardeningProperties properties,
            RequestMetadataExtractor metadataExtractor,
            SecurityAuditService securityAuditService
    ) {
        this.properties = properties.getLogin();
        this.rateLimitProperties = properties.getRateLimit();
        this.metadataExtractor = metadataExtractor;
        this.securityAuditService = securityAuditService;
    }

    public void assertAllowed(String email, HttpServletRequest request) {
        if (!properties.isProgressiveLockEnabled()) {
            return;
        }
        pruneIfNeeded();
        String key = key(email, request);
        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }
        Instant now = Instant.now();
        if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
            long retryAfterSeconds = state.lockedUntil.getEpochSecond() - now.getEpochSecond();
            securityAuditService.record("LOGIN_TEMPORARILY_LOCKED", normalizeEmail(email), false, request,
                    "Muitas tentativas de login malsucedidas.");
            throw new RateLimitExceededException("Muitas tentativas de login. Tente novamente mais tarde.", retryAfterSeconds);
        }
        if (state.firstFailureAt != null && state.firstFailureAt.plusSeconds(properties.getFailureWindowSeconds()).isBefore(now)) {
            attempts.remove(key);
        }
    }

    public void recordFailure(String email, HttpServletRequest request) {
        if (!properties.isProgressiveLockEnabled()) {
            return;
        }
        pruneIfNeeded();
        String key = key(email, request);
        Instant now = Instant.now();
        AttemptState state = attempts.compute(key, (ignored, current) -> {
            AttemptState updated = current == null ? new AttemptState() : current;
            if (updated.firstFailureAt == null || updated.firstFailureAt.plusSeconds(properties.getFailureWindowSeconds()).isBefore(now)) {
                updated.firstFailureAt = now;
                updated.failures = 0;
                updated.lockCount = 0;
                updated.lockedUntil = null;
            }
            updated.failures++;
            if (updated.failures >= properties.getMaxFailures()) {
                updated.lockCount++;
                long lockSeconds = Math.min(properties.getMaxLockSeconds(), properties.getBaseLockSeconds() * updated.lockCount);
                updated.lockedUntil = now.plusSeconds(lockSeconds);
                updated.failures = 0;
                updated.firstFailureAt = now;
            }
            return updated;
        });
        String details = state.lockedUntil != null && state.lockedUntil.isAfter(now)
                ? "Falha de login; chave temporariamente bloqueada."
                : "Falha de login.";
        securityAuditService.record("LOGIN_FAILURE", normalizeEmail(email), false, request, details);
    }

    public void recordSuccess(String email, HttpServletRequest request) {
        attempts.remove(key(email, request));
        securityAuditService.record("LOGIN_SUCCESS", normalizeEmail(email), true, request, "Login bem-sucedido.");
    }

    private String key(String email, HttpServletRequest request) {
        return normalizeEmail(email) + "|" + metadataExtractor.clientIp(request);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void pruneIfNeeded() {
        int maxTrackedKeys = Math.max(1000, rateLimitProperties.getMaxTrackedKeys());
        if (attempts.size() <= maxTrackedKeys) {
            return;
        }
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            boolean lockExpired = state.lockedUntil == null || state.lockedUntil.isBefore(now);
            boolean windowExpired = state.firstFailureAt == null || state.firstFailureAt.plusSeconds(properties.getFailureWindowSeconds()).isBefore(now);
            return lockExpired && windowExpired;
        });
    }

    private static class AttemptState {
        private Instant firstFailureAt;
        private int failures;
        private int lockCount;
        private Instant lockedUntil;
    }
}
