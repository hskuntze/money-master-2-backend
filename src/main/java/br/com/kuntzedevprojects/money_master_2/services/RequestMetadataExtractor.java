package br.com.kuntzedevprojects.money_master_2.services;

import java.security.Principal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestMetadataExtractor {

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token", "access_token", "refresh_token", "refreshtoken", "password", "senha", "secret", "authorization"
    );

    private final SecurityHardeningProperties.Proxy proxyProperties;

    public RequestMetadataExtractor(SecurityHardeningProperties properties) {
        this.proxyProperties = properties.getProxy();
    }

    public String principal(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Principal principal = request.getUserPrincipal();
        return principal == null ? null : principal.getName();
    }

    public String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteAddr = safeTrim(request.getRemoteAddr(), 80);
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        if (proxyProperties.isTrustCloudflareConnectingIp()) {
            String cfConnectingIp = safeTrim(request.getHeader("CF-Connecting-IP"), 80);
            if (hasText(cfConnectingIp)) {
                return cfConnectingIp;
            }
        }

        String forwardedFor = safeTrim(request.getHeader("X-Forwarded-For"), 500);
        if (hasText(forwardedFor)) {
            return safeTrim(forwardedFor.split(",")[0], 80);
        }

        String realIp = safeTrim(request.getHeader("X-Real-IP"), 80);
        if (hasText(realIp)) {
            return realIp;
        }
        return remoteAddr;
    }

    public String userAgent(HttpServletRequest request) {
        return request == null ? null : safeTrim(request.getHeader("User-Agent"), 500);
    }

    public String safeQueryString(HttpServletRequest request) {
        if (request == null || !hasText(request.getQueryString())) {
            return null;
        }
        return Arrays.stream(request.getQueryString().split("&"))
                .map(this::redactQueryPair)
                .collect(Collectors.joining("&"));
    }

    private String redactQueryPair(String pair) {
        int separator = pair.indexOf('=');
        String key = separator >= 0 ? pair.substring(0, separator) : pair;
        String normalizedKey = key.trim().toLowerCase(Locale.ROOT);
        if (SENSITIVE_QUERY_KEYS.contains(normalizedKey)) {
            return key + "=[REDACTED]";
        }
        return safeTrim(pair, 1000);
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (!hasText(remoteAddr) || proxyProperties.getTrustedProxyAddresses() == null) {
            return false;
        }
        return proxyProperties.getTrustedProxyAddresses().stream()
                .filter(this::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(remoteAddr.toLowerCase(Locale.ROOT)));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeTrim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
