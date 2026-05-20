package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarYieldCalculationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "tb_savings_jar",
        indexes = {
                @Index(name = "idx_savings_jar_owner", columnList = "owner_id"),
                @Index(name = "idx_savings_jar_owner_bank_name", columnList = "owner_id,institution_name,name"),
                @Index(name = "idx_savings_jar_yield_enabled", columnList = "yield_enabled,active,last_yield_calculation_date")
        }
)
public class SavingsJar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id")
    private Account linkedAccount;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String institutionName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount = BigDecimal.ZERO;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean yieldEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SavingsJarYieldCalculationType yieldCalculationType = SavingsJarYieldCalculationType.MANUAL;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal yieldPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean businessDaysOnly = true;

    @Column(nullable = false)
    private boolean useBrazilianHolidays = false;

    @Column(name = "yield_start_date")
    private LocalDate yieldStartDate;

    @Column(name = "last_yield_calculation_date")
    private LocalDate lastYieldCalculationDate;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Account getLinkedAccount() {
        return linkedAccount;
    }

    public void setLinkedAccount(Account linkedAccount) {
        this.linkedAccount = linkedAccount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isYieldEnabled() {
        return yieldEnabled;
    }

    public void setYieldEnabled(boolean yieldEnabled) {
        this.yieldEnabled = yieldEnabled;
    }

    public SavingsJarYieldCalculationType getYieldCalculationType() {
        return yieldCalculationType;
    }

    public void setYieldCalculationType(SavingsJarYieldCalculationType yieldCalculationType) {
        this.yieldCalculationType = yieldCalculationType;
    }

    public BigDecimal getYieldPercentage() {
        return yieldPercentage;
    }

    public void setYieldPercentage(BigDecimal yieldPercentage) {
        this.yieldPercentage = yieldPercentage;
    }

    public boolean isBusinessDaysOnly() {
        return businessDaysOnly;
    }

    public void setBusinessDaysOnly(boolean businessDaysOnly) {
        this.businessDaysOnly = businessDaysOnly;
    }

    public boolean isUseBrazilianHolidays() {
        return useBrazilianHolidays;
    }

    public void setUseBrazilianHolidays(boolean useBrazilianHolidays) {
        this.useBrazilianHolidays = useBrazilianHolidays;
    }

    public LocalDate getYieldStartDate() {
        return yieldStartDate;
    }

    public void setYieldStartDate(LocalDate yieldStartDate) {
        this.yieldStartDate = yieldStartDate;
    }

    public LocalDate getLastYieldCalculationDate() {
        return lastYieldCalculationDate;
    }

    public void setLastYieldCalculationDate(LocalDate lastYieldCalculationDate) {
        this.lastYieldCalculationDate = lastYieldCalculationDate;
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
