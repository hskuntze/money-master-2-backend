package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_email_settings")
public class EmailSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 180)
    private String host;

    private Integer port;

    @Column(length = 180)
    private String username;

    @Column(length = 500)
    private String password;

    @Column(length = 180)
    private String fromAddress;

    @Column(nullable = false)
    private boolean smtpAuth = true;

    @Column(nullable = false)
    private boolean startTlsEnable = true;

    @Column(nullable = false)
    private boolean startTlsRequired = true;

    @Column(nullable = false)
    private boolean sslEnable = false;

    @Column(nullable = false)
    private boolean debug = false;

    private Integer connectionTimeoutMs;

    private Integer timeoutMs;

    private Integer writeTimeoutMs;

    @Column(length = 500)
    private String confirmationBaseUrl;

    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = normalize(host);
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = normalize(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = normalize(fromAddress);
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public boolean isStartTlsEnable() {
        return startTlsEnable;
    }

    public void setStartTlsEnable(boolean startTlsEnable) {
        this.startTlsEnable = startTlsEnable;
    }

    public boolean isStartTlsRequired() {
        return startTlsRequired;
    }

    public void setStartTlsRequired(boolean startTlsRequired) {
        this.startTlsRequired = startTlsRequired;
    }

    public boolean isSslEnable() {
        return sslEnable;
    }

    public void setSslEnable(boolean sslEnable) {
        this.sslEnable = sslEnable;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getWriteTimeoutMs() {
        return writeTimeoutMs;
    }

    public void setWriteTimeoutMs(Integer writeTimeoutMs) {
        this.writeTimeoutMs = writeTimeoutMs;
    }

    public String getConfirmationBaseUrl() {
        return confirmationBaseUrl;
    }

    public void setConfirmationBaseUrl(String confirmationBaseUrl) {
        this.confirmationBaseUrl = normalize(confirmationBaseUrl);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
