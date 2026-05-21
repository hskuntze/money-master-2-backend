package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "tb_savings_jar_movement",
        indexes = {
                @Index(name = "idx_savings_jar_movement_jar_date", columnList = "savings_jar_id,occurred_on"),
                @Index(name = "idx_savings_jar_movement_type", columnList = "movement_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_savings_jar_movement_reference_key", columnNames = {"savings_jar_id", "reference_key"})
        }
)
public class SavingsJarMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_jar_id", nullable = false)
    private SavingsJar savingsJar;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private SavingsJarMovementType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionSource source = TransactionSource.MANUAL;

    @Column(precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(precision = 12, scale = 6)
    private BigDecimal rateApplied;

    @Column(length = 80)
    private String rateReference;

    @Column(name = "reference_key", length = 80)
    private String referenceKey;

    @Column(length = 2000)
    private String aiRawMessage;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SavingsJar getSavingsJar() {
        return savingsJar;
    }

    public void setSavingsJar(SavingsJar savingsJar) {
        this.savingsJar = savingsJar;
    }

    public SavingsJarMovementType getType() {
        return type;
    }

    public void setType(SavingsJarMovementType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionSource getSource() {
        return source;
    }

    public void setSource(TransactionSource source) {
        this.source = source;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getRateApplied() {
        return rateApplied;
    }

    public void setRateApplied(BigDecimal rateApplied) {
        this.rateApplied = rateApplied;
    }

    public String getRateReference() {
        return rateReference;
    }

    public void setRateReference(String rateReference) {
        this.rateReference = rateReference;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(String referenceKey) {
        this.referenceKey = referenceKey;
    }

    public String getAiRawMessage() {
        return aiRawMessage;
    }

    public void setAiRawMessage(String aiRawMessage) {
        this.aiRawMessage = aiRawMessage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
