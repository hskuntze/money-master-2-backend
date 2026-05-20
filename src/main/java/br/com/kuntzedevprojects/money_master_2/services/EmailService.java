package br.com.kuntzedevprojects.money_master_2.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import br.com.kuntzedevprojects.money_master_2.config.properties.MoneyMasterEmailProperties;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MoneyMasterEmailProperties properties;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, MoneyMasterEmailProperties properties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    public void sendConfirmationEmail(User user, String rawToken) {
        String confirmationUrl = properties.getConfirmationBaseUrl() + "?token=" + rawToken;
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationName", properties.getApplicationName());
        variables.put("name", user.getName());
        variables.put("confirmationUrl", confirmationUrl);

        sendHtml(user.getEmail(), "Confirme seu cadastro - " + properties.getApplicationName(), "email-confirmation", variables);
    }

    public void sendHtml(String to, String subject, String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Não foi possível montar o e-mail.", ex);
        } catch (MailException ex) {
            throw new IllegalStateException("Não foi possível enviar o e-mail.", ex);
        }
    }
}
