package br.com.kuntzedevprojects.money_master_2.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public class MoneyMasterEmailProperties {

    private String from = "no-reply@moneymaster.local";
    private String confirmationBaseUrl = "http://localhost:3000/confirm-email";
    private String applicationName = "Money Master";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getConfirmationBaseUrl() {
        return confirmationBaseUrl;
    }

    public void setConfirmationBaseUrl(String confirmationBaseUrl) {
        this.confirmationBaseUrl = confirmationBaseUrl;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
