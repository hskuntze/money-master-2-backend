package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_failure_log")
public class FailureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 12)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 1000)
    private String queryString;

    @Column(nullable = false)
    private int statusCode;

    @Column(length = 180)
    private String principal;

    @Column(length = 80)
    private String clientIp;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false, length = 255)
    private String exceptionClass;

    @Column(nullable = false, length = 2000)
    private String message;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stackTrace;

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

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = trim(queryString, 1000);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
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

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = trim(exceptionClass, 255);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = trim(message, 2000);
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
