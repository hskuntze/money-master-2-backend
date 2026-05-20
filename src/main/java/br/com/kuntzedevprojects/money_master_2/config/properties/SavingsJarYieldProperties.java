package br.com.kuntzedevprojects.money_master_2.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-master.savings-jar.yield")
public class SavingsJarYieldProperties {

    private boolean applyOnStartup = true;
    private boolean failStartupOnError = false;
    private String zoneId = "America/Sao_Paulo";
    private int defaultLookbackDays = 10;

    public boolean isApplyOnStartup() {
        return applyOnStartup;
    }

    public void setApplyOnStartup(boolean applyOnStartup) {
        this.applyOnStartup = applyOnStartup;
    }

    public boolean isFailStartupOnError() {
        return failStartupOnError;
    }

    public void setFailStartupOnError(boolean failStartupOnError) {
        this.failStartupOnError = failStartupOnError;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public int getDefaultLookbackDays() {
        return defaultLookbackDays;
    }

    public void setDefaultLookbackDays(int defaultLookbackDays) {
        this.defaultLookbackDays = defaultLookbackDays;
    }
}
