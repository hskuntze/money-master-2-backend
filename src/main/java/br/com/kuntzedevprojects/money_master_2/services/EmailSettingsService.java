package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.admin.EmailSettingsResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.EmailSettingsUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.EmailSettings;
import br.com.kuntzedevprojects.money_master_2.repositories.EmailSettingsRepository;

@Service
public class EmailSettingsService {

    private static final Long SINGLETON_ID = 1L;

    private final EmailSettingsRepository repository;
    private final Environment environment;

    public EmailSettingsService(EmailSettingsRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    @Transactional(readOnly = true)
    public EmailSettingsResponse getSettings() {
        return EmailSettingsResponse.from(resolve());
    }

    @Transactional
    public EmailSettingsResponse update(EmailSettingsUpdateRequest request) {
        EmailSettings settings = repository.findById(SINGLETON_ID).orElseGet(() -> {
            EmailSettings created = new EmailSettings();
            created.setId(SINGLETON_ID);
            return created;
        });

        settings.setEnabled(Boolean.TRUE.equals(request.enabled()));
        settings.setHost(request.host());
        settings.setPort(request.port());
        settings.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            settings.setPassword(request.password());
        }
        settings.setFromAddress(request.fromAddress());
        settings.setSmtpAuth(Boolean.TRUE.equals(request.smtpAuth()));
        settings.setStartTlsEnable(Boolean.TRUE.equals(request.startTlsEnable()));
        settings.setStartTlsRequired(Boolean.TRUE.equals(request.startTlsRequired()));
        settings.setSslEnable(Boolean.TRUE.equals(request.sslEnable()));
        settings.setDebug(Boolean.TRUE.equals(request.debug()));
        settings.setConnectionTimeoutMs(defaultIfNull(request.connectionTimeoutMs(), 10000));
        settings.setTimeoutMs(defaultIfNull(request.timeoutMs(), 10000));
        settings.setWriteTimeoutMs(defaultIfNull(request.writeTimeoutMs(), 10000));
        settings.setConfirmationBaseUrl(request.confirmationBaseUrl());
        settings.setUpdatedAt(Instant.now());

        repository.save(settings);
        return EmailSettingsResponse.from(resolve(settings));
    }

    @Transactional(readOnly = true)
    public ResolvedEmailSettings resolve() {
        Optional<EmailSettings> settings = repository.findById(SINGLETON_ID);
        return settings.map(this::resolve).orElseGet(this::resolveFromEnvironment);
    }

    private ResolvedEmailSettings resolve(EmailSettings settings) {
        ResolvedEmailSettings env = resolveFromEnvironment();
        return new ResolvedEmailSettings(
                settings.isEnabled(),
                valueOr(settings.getHost(), env.host()),
                defaultIfNull(settings.getPort(), env.port()),
                valueOr(settings.getUsername(), env.username()),
                valueOr(settings.getPassword(), env.password()),
                valueOr(settings.getFromAddress(), env.fromAddress()),
                settings.isSmtpAuth(),
                settings.isStartTlsEnable(),
                settings.isStartTlsRequired(),
                settings.isSslEnable(),
                settings.isDebug(),
                defaultIfNull(settings.getConnectionTimeoutMs(), env.connectionTimeoutMs()),
                defaultIfNull(settings.getTimeoutMs(), env.timeoutMs()),
                defaultIfNull(settings.getWriteTimeoutMs(), env.writeTimeoutMs()),
                valueOr(settings.getConfirmationBaseUrl(), env.confirmationBaseUrl()),
                settings.getUpdatedAt(),
                true
        );
    }

    private ResolvedEmailSettings resolveFromEnvironment() {
        String username = property("spring.mail.username", "");
        return new ResolvedEmailSettings(
                true,
                property("spring.mail.host", "smtp.gmail.com"),
                intProperty("spring.mail.port", 587),
                username,
                property("spring.mail.password", ""),
                valueOr(property("money-master.email.from", ""), username),
                boolProperty("spring.mail.properties.mail.smtp.auth", true),
                boolProperty("spring.mail.properties.mail.smtp.starttls.enable", true),
                boolProperty("spring.mail.properties.mail.smtp.starttls.required", true),
                boolProperty("spring.mail.properties.mail.smtp.ssl.enable", false),
                boolProperty("spring.mail.properties.mail.debug", false),
                intProperty("spring.mail.properties.mail.smtp.connectiontimeout", 10000),
                intProperty("spring.mail.properties.mail.smtp.timeout", 10000),
                intProperty("spring.mail.properties.mail.smtp.writetimeout", 10000),
                property("money-master.email.confirmation-base-url", "http://localhost:8080/api/auth/confirm-email"),
                null,
                false
        );
    }

    private String property(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    private int intProperty(String key, int defaultValue) {
        Integer value = environment.getProperty(key, Integer.class);
        return value == null ? defaultValue : value;
    }

    private boolean boolProperty(String key, boolean defaultValue) {
        Boolean value = environment.getProperty(key, Boolean.class);
        return value == null ? defaultValue : value;
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer defaultIfNull(Integer value, Integer fallback) {
        return Objects.requireNonNullElse(value, fallback);
    }

    public record ResolvedEmailSettings(
            boolean enabled,
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            boolean smtpAuth,
            boolean startTlsEnable,
            boolean startTlsRequired,
            boolean sslEnable,
            boolean debug,
            Integer connectionTimeoutMs,
            Integer timeoutMs,
            Integer writeTimeoutMs,
            String confirmationBaseUrl,
            Instant updatedAt,
            boolean usingDatabaseSettings
    ) {
    }
}
