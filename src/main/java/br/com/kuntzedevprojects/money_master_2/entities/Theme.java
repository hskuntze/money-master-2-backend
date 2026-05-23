package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_theme")
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, length = 120)
    private String appName;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String faviconUrl;

    @Column(nullable = false, length = 20)
    private String primaryColor;

    @Column(nullable = false, length = 20)
    private String secondaryColor;

    @Column(nullable = false, length = 20)
    private String accentColor;

    @Column(nullable = false, length = 20)
    private String backgroundColor;

    @Column(nullable = false, length = 20)
    private String textColor;

    @Column(nullable = false, length = 20)
    private String cardColor;

    @Column(length = 140)
    private String loginTitle;

    @Column(length = 255)
    private String loginSubtitle;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Theme defaultTheme() {
        Theme theme = new Theme();
        theme.setAppName("Money Master");
        theme.setPrimaryColor("#2563eb");
        theme.setSecondaryColor("#0f172a");
        theme.setAccentColor("#22c55e");
        theme.setBackgroundColor("#f8fafc");
        theme.setTextColor("#0f172a");
        theme.setCardColor("#ffffff");
        theme.setLoginTitle("Controle financeiro inteligente");
        theme.setLoginSubtitle("Organize gastos, receitas e decisões com apoio de IA.");
        theme.setActive(true);
        return theme;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getCardColor() {
        return cardColor;
    }

    public void setCardColor(String cardColor) {
        this.cardColor = cardColor;
    }

    public String getLoginTitle() {
        return loginTitle;
    }

    public void setLoginTitle(String loginTitle) {
        this.loginTitle = loginTitle;
    }

    public String getLoginSubtitle() {
        return loginSubtitle;
    }

    public void setLoginSubtitle(String loginSubtitle) {
        this.loginSubtitle = loginSubtitle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
