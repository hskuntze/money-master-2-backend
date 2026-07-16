package br.com.kuntzedevprojects.money_master_2.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-master.ai.finance")
public class FinanceAiProperties {

    private String defaultAccountName = "Conta principal";
    private String defaultAccountType = "CHECKING";
    private String locale = "pt-BR";
    private String zoneId = "America/Sao_Paulo";
    private long confirmationTokenMinutes = 10;

    public String getDefaultAccountName() {
        return defaultAccountName;
    }

    public void setDefaultAccountName(String defaultAccountName) {
        this.defaultAccountName = defaultAccountName;
    }

    public String getDefaultAccountType() {
        return defaultAccountType;
    }

    public void setDefaultAccountType(String defaultAccountType) {
        this.defaultAccountType = defaultAccountType;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public long getConfirmationTokenMinutes() {
        return confirmationTokenMinutes;
    }

    public void setConfirmationTokenMinutes(long confirmationTokenMinutes) {
        this.confirmationTokenMinutes = confirmationTokenMinutes;
    }
}
