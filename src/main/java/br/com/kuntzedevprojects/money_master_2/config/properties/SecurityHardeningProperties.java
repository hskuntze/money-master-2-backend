package br.com.kuntzedevprojects.money_master_2.config.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.hardening")
public class SecurityHardeningProperties {

    private final RateLimit rateLimit = new RateLimit();
    private final Login login = new Login();
    private final Proxy proxy = new Proxy();
    private final Headers headers = new Headers();
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Login getLogin() {
        return login;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public Headers getHeaders() {
        return headers;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int loginLimit = 8;
        private long loginWindowSeconds = 300;
        private int registerLimit = 5;
        private long registerWindowSeconds = 3600;
        private int refreshLimit = 30;
        private long refreshWindowSeconds = 300;
        private int emailConfirmationLimit = 10;
        private long emailConfirmationWindowSeconds = 3600;
        private int aiChatLimit = 20;
        private long aiChatWindowSeconds = 300;
        private int defaultWriteLimit = 120;
        private long defaultWriteWindowSeconds = 300;
        private int maxTrackedKeys = 20000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getLoginLimit() {
            return loginLimit;
        }

        public void setLoginLimit(int loginLimit) {
            this.loginLimit = loginLimit;
        }

        public long getLoginWindowSeconds() {
            return loginWindowSeconds;
        }

        public void setLoginWindowSeconds(long loginWindowSeconds) {
            this.loginWindowSeconds = loginWindowSeconds;
        }

        public int getRegisterLimit() {
            return registerLimit;
        }

        public void setRegisterLimit(int registerLimit) {
            this.registerLimit = registerLimit;
        }

        public long getRegisterWindowSeconds() {
            return registerWindowSeconds;
        }

        public void setRegisterWindowSeconds(long registerWindowSeconds) {
            this.registerWindowSeconds = registerWindowSeconds;
        }

        public int getRefreshLimit() {
            return refreshLimit;
        }

        public void setRefreshLimit(int refreshLimit) {
            this.refreshLimit = refreshLimit;
        }

        public long getRefreshWindowSeconds() {
            return refreshWindowSeconds;
        }

        public void setRefreshWindowSeconds(long refreshWindowSeconds) {
            this.refreshWindowSeconds = refreshWindowSeconds;
        }

        public int getEmailConfirmationLimit() {
            return emailConfirmationLimit;
        }

        public void setEmailConfirmationLimit(int emailConfirmationLimit) {
            this.emailConfirmationLimit = emailConfirmationLimit;
        }

        public long getEmailConfirmationWindowSeconds() {
            return emailConfirmationWindowSeconds;
        }

        public void setEmailConfirmationWindowSeconds(long emailConfirmationWindowSeconds) {
            this.emailConfirmationWindowSeconds = emailConfirmationWindowSeconds;
        }

        public int getAiChatLimit() {
            return aiChatLimit;
        }

        public void setAiChatLimit(int aiChatLimit) {
            this.aiChatLimit = aiChatLimit;
        }

        public long getAiChatWindowSeconds() {
            return aiChatWindowSeconds;
        }

        public void setAiChatWindowSeconds(long aiChatWindowSeconds) {
            this.aiChatWindowSeconds = aiChatWindowSeconds;
        }

        public int getDefaultWriteLimit() {
            return defaultWriteLimit;
        }

        public void setDefaultWriteLimit(int defaultWriteLimit) {
            this.defaultWriteLimit = defaultWriteLimit;
        }

        public long getDefaultWriteWindowSeconds() {
            return defaultWriteWindowSeconds;
        }

        public void setDefaultWriteWindowSeconds(long defaultWriteWindowSeconds) {
            this.defaultWriteWindowSeconds = defaultWriteWindowSeconds;
        }

        public int getMaxTrackedKeys() {
            return maxTrackedKeys;
        }

        public void setMaxTrackedKeys(int maxTrackedKeys) {
            this.maxTrackedKeys = maxTrackedKeys;
        }
    }

    public static class Login {
        private boolean progressiveLockEnabled = true;
        private int maxFailures = 6;
        private long failureWindowSeconds = 900;
        private long baseLockSeconds = 300;
        private long maxLockSeconds = 3600;

        public boolean isProgressiveLockEnabled() {
            return progressiveLockEnabled;
        }

        public void setProgressiveLockEnabled(boolean progressiveLockEnabled) {
            this.progressiveLockEnabled = progressiveLockEnabled;
        }

        public int getMaxFailures() {
            return maxFailures;
        }

        public void setMaxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
        }

        public long getFailureWindowSeconds() {
            return failureWindowSeconds;
        }

        public void setFailureWindowSeconds(long failureWindowSeconds) {
            this.failureWindowSeconds = failureWindowSeconds;
        }

        public long getBaseLockSeconds() {
            return baseLockSeconds;
        }

        public void setBaseLockSeconds(long baseLockSeconds) {
            this.baseLockSeconds = baseLockSeconds;
        }

        public long getMaxLockSeconds() {
            return maxLockSeconds;
        }

        public void setMaxLockSeconds(long maxLockSeconds) {
            this.maxLockSeconds = maxLockSeconds;
        }
    }

    public static class Proxy {
        private List<String> trustedProxyAddresses = new ArrayList<>(List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));
        private boolean trustCloudflareConnectingIp = true;

        public List<String> getTrustedProxyAddresses() {
            return trustedProxyAddresses;
        }

        public void setTrustedProxyAddresses(List<String> trustedProxyAddresses) {
            this.trustedProxyAddresses = trustedProxyAddresses;
        }

        public boolean isTrustCloudflareConnectingIp() {
            return trustCloudflareConnectingIp;
        }

        public void setTrustCloudflareConnectingIp(boolean trustCloudflareConnectingIp) {
            this.trustCloudflareConnectingIp = trustCloudflareConnectingIp;
        }
    }

    public static class Headers {
        private String contentSecurityPolicy = "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'";
        private String permissionsPolicy = "camera=(), microphone=(), geolocation=(), payment=()";
        private boolean hstsEnabled = false;
        private long hstsMaxAgeSeconds = 31536000;

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public long getHstsMaxAgeSeconds() {
            return hstsMaxAgeSeconds;
        }

        public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
            this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        }
    }

    public static class PasswordPolicy {
        private int minLength = 10;
        private int maxLength = 80;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public boolean isRequireUppercase() {
            return requireUppercase;
        }

        public void setRequireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
        }

        public boolean isRequireLowercase() {
            return requireLowercase;
        }

        public void setRequireLowercase(boolean requireLowercase) {
            this.requireLowercase = requireLowercase;
        }

        public boolean isRequireDigit() {
            return requireDigit;
        }

        public void setRequireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
        }

        public boolean isRequireSpecial() {
            return requireSpecial;
        }

        public void setRequireSpecial(boolean requireSpecial) {
            this.requireSpecial = requireSpecial;
        }
    }
}
