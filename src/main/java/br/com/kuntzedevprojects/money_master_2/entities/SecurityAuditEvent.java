package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "tb_security_audit_event",
        indexes = {
                @Index(name = "idx_security_event_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_security_event_type", columnList = "event_type"),
                @Index(name = "idx_security_event_principal", columnList = "principal"),
                @Index(name = "idx_security_event_client_ip", columnList = "client_ip")
        }
)
public class SecurityAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(length = 180)
    private String principal;

    @Column(name = "client_ip", length = 80)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 12)
    private String method;

    @Column(length = 500)
    private String path;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 1000)
    private String details;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = trim(eventType, 80);
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = trim(principal, 180);
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = trim(clientIp, 80);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = trim(userAgent, 500);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = trim(method, 12);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = trim(path, 500);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = trim(details, 1000);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
