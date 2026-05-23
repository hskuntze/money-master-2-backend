package br.com.kuntzedevprojects.money_master_2.services;

import java.security.Principal;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestMetadataExtractor {

    public String principal(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal == null ? null : principal.getName();
    }

    public String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
