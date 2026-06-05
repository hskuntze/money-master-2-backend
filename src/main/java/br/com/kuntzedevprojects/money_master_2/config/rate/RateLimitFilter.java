package br.com.kuntzedevprojects.money_master_2.config.rate;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;
import br.com.kuntzedevprojects.money_master_2.exceptions.RateLimitExceededException;
import br.com.kuntzedevprojects.money_master_2.services.RequestMetadataExtractor;
import br.com.kuntzedevprojects.money_master_2.services.SecurityAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final SecurityHardeningProperties.RateLimit properties;
    private final RequestMetadataExtractor metadataExtractor;
    private final SecurityAuditService securityAuditService;

    public RateLimitFilter(
            SecurityHardeningProperties properties,
            RequestMetadataExtractor metadataExtractor,
            SecurityAuditService securityAuditService
    ) {
        this.properties = properties.getRateLimit();
        this.metadataExtractor = metadataExtractor;
        this.securityAuditService = securityAuditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Limit limit = resolveLimit(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        pruneIfNeeded();
        String key = limit.name() + "|" + metadataExtractor.clientIp(request);
        Instant now = Instant.now();
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || current.windowStartedAt.plusSeconds(limit.windowSeconds()).isBefore(now)) {
                return new Bucket(now, 1);
            }
            current.count++;
            return current;
        });

        if (bucket.count > limit.maxRequests()) {
            long retryAfterSeconds = Math.max(1, bucket.windowStartedAt.plusSeconds(limit.windowSeconds()).getEpochSecond() - now.getEpochSecond());
            securityAuditService.record("RATE_LIMIT_EXCEEDED", null, false, request,
                    "Limite excedido para " + limit.name() + ".");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Muitas requisições. Tente novamente mais tarde.\"}");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException ex) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Muitas requisições. Tente novamente mais tarde.\"}");
        }
    }

    private Limit resolveLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (HttpMethod.POST.matches(method) && "/auth/login".equals(path)) {
            return new Limit("auth-login", properties.getLoginLimit(), properties.getLoginWindowSeconds());
        }
        if (HttpMethod.POST.matches(method) && "/auth/register".equals(path)) {
            return new Limit("auth-register", properties.getRegisterLimit(), properties.getRegisterWindowSeconds());
        }
        if (HttpMethod.POST.matches(method) && "/auth/refresh".equals(path)) {
            return new Limit("auth-refresh", properties.getRefreshLimit(), properties.getRefreshWindowSeconds());
        }
        if (HttpMethod.POST.matches(method) && "/auth/resend-confirmation".equals(path)) {
            return new Limit("auth-resend-confirmation", properties.getEmailConfirmationLimit(), properties.getEmailConfirmationWindowSeconds());
        }
        if (HttpMethod.POST.matches(method) && "/ai/chat".equals(path)) {
            return new Limit("ai-chat", properties.getAiChatLimit(), properties.getAiChatWindowSeconds());
        }
        if ((HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method))
                && !path.startsWith("/auth/")) {
            return new Limit("default-write", properties.getDefaultWriteLimit(), properties.getDefaultWriteWindowSeconds());
        }
        return null;
    }

    private void pruneIfNeeded() {
        int maxTrackedKeys = Math.max(1000, properties.getMaxTrackedKeys());
        if (buckets.size() <= maxTrackedKeys) {
            return;
        }
        Instant now = Instant.now();
        buckets.entrySet().removeIf(entry -> entry.getValue().windowStartedAt.plusSeconds(3600).isBefore(now));
    }

    private record Limit(String name, int maxRequests, long windowSeconds) {
    }

    private static class Bucket {
        private final Instant windowStartedAt;
        private int count;

        private Bucket(Instant windowStartedAt, int count) {
            this.windowStartedAt = windowStartedAt;
            this.count = count;
        }
    }
}
