package br.com.kuntzedevprojects.money_master_2.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import br.com.kuntzedevprojects.money_master_2.config.properties.MoneyMasterEmailProperties;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.EmailSettingsService.ResolvedEmailSettings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final TemplateEngine templateEngine;
    private final MoneyMasterEmailProperties properties;
    private final EmailSettingsService emailSettingsService;

    public EmailService(
            TemplateEngine templateEngine,
            MoneyMasterEmailProperties properties,
            EmailSettingsService emailSettingsService
    ) {
        this.templateEngine = templateEngine;
        this.properties = properties;
        this.emailSettingsService = emailSettingsService;
    }

    public void sendConfirmationEmail(User user, String rawToken) {
        ResolvedEmailSettings settings = emailSettingsService.resolve();
        String baseUrl = settings.confirmationBaseUrl() == null || settings.confirmationBaseUrl().isBlank()
                ? properties.getConfirmationBaseUrl()
                : settings.confirmationBaseUrl();
        String confirmationUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "token=" + rawToken;

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationName", properties.getApplicationName());
        variables.put("name", user.getName());
        variables.put("confirmationUrl", confirmationUrl);

        sendHtml(user.getEmail(), "Confirme seu cadastro - " + properties.getApplicationName(), "email-confirmation", variables);
    }

    public void sendTestEmail(String to) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationName", properties.getApplicationName());
        variables.put("name", "Administrador");
        variables.put("confirmationUrl", "#");
        sendHtml(to, "Teste de envio - " + properties.getApplicationName(), "email-confirmation", variables);
    }

    public void sendHtml(String to, String subject, String template, Map<String, Object> variables) {
        ResolvedEmailSettings settings = emailSettingsService.resolve();
        if (!settings.enabled()) {
            throw new BusinessException("O envio de e-mail está desabilitado nas configurações do sistema.");
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(template, context);

            JavaMailSenderImpl mailSender = buildMailSender(settings);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveFrom(settings));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Não foi possível montar o e-mail.", ex);
        } catch (MailException ex) {
            throw new IllegalStateException("Não foi possível enviar o e-mail. Verifique host, porta, usuário, senha, remetente e TLS/SSL.", ex);
        }
    }

    private JavaMailSenderImpl buildMailSender(ResolvedEmailSettings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());
        sender.setDefaultEncoding("UTF-8");

        Properties javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.smtp.auth", String.valueOf(settings.smtpAuth()));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(settings.startTlsEnable()));
        javaMailProperties.put("mail.smtp.starttls.required", String.valueOf(settings.startTlsRequired()));
        javaMailProperties.put("mail.smtp.ssl.enable", String.valueOf(settings.sslEnable()));
        javaMailProperties.put("mail.debug", String.valueOf(settings.debug()));
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(settings.connectionTimeoutMs()));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(settings.timeoutMs()));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(settings.writeTimeoutMs()));
        return sender;
    }

    private String resolveFrom(ResolvedEmailSettings settings) {
        if (settings.fromAddress() != null && !settings.fromAddress().isBlank()) {
            return settings.fromAddress();
        }
        if (settings.username() != null && !settings.username().isBlank()) {
            return settings.username();
        }
        return properties.getFrom();
    }
}
