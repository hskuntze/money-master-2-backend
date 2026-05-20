package br.com.kuntzedevprojects.money_master_2.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /**
     * HS256 secret. Use at least 32 characters. Prefer a random 64+ char value in production.
     */
    private String secret = "CHANGE_ME_USE_A_RANDOM_64_CHARACTER_SECRET_FOR_LOCAL_ONLY";
    private String issuer = "money-master-2";
    private long accessTokenMinutes = 30;
    private long refreshTokenDays = 15;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }
}
