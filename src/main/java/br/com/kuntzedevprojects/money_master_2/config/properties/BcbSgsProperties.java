package br.com.kuntzedevprojects.money_master_2.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-master.integrations.bcb.sgs")
public class BcbSgsProperties {

    private String baseUrl = "https://api.bcb.gov.br/dados/serie/bcdata.sgs";
    private Integer cdiSeriesCode = 12;
    private Integer latestValuesLimit = 20;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getCdiSeriesCode() {
        return cdiSeriesCode;
    }

    public void setCdiSeriesCode(Integer cdiSeriesCode) {
        this.cdiSeriesCode = cdiSeriesCode;
    }

    public Integer getLatestValuesLimit() {
        return latestValuesLimit;
    }

    public void setLatestValuesLimit(Integer latestValuesLimit) {
        this.latestValuesLimit = latestValuesLimit;
    }
}
