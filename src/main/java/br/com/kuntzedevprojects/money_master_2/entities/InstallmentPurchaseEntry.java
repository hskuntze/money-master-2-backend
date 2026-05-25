package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentSource;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_installment_purchase_entry", indexes = {
        @Index(name = "idx_installment_entry_owner_due", columnList = "owner_id,due_date"),
        @Index(name = "idx_installment_entry_period", columnList = "financial_period_id"),
        @Index(name = "idx_installment_entry_plan_item", columnList = "monthly_plan_item_id"),
        @Index(name = "idx_installment_entry_status_source", columnList = "status,payment_source") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_installment_entry_purchase_number", columnNames = { "purchase_id",
                        "installment_number" }) })
public class InstallmentPurchaseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private InstallmentPurchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_period_id", nullable = false)
    private FinancialPeriod financialPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_plan_item_id")
    private MonthlyPlanItem monthlyPlanItem;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstallmentEntryStatus status = InstallmentEntryStatus.PENDING;

    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstallmentPaymentSource paymentSource = InstallmentPaymentSource.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id")
    private User paidBy;

    private Instant paymentRegisteredAt;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        if (this.paymentSource == null) {
            this.paymentSource = InstallmentPaymentSource.NONE;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
        if (this.paymentSource == null) {
            this.paymentSource = InstallmentPaymentSource.NONE;
        }
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

    public InstallmentPurchase getPurchase() {
        return purchase;
    }

    public void setPurchase(InstallmentPurchase purchase) {
        this.purchase = purchase;
    }

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

    public MonthlyPlanItem getMonthlyPlanItem() {
        return monthlyPlanItem;
    }

    public void setMonthlyPlanItem(MonthlyPlanItem monthlyPlanItem) {
        this.monthlyPlanItem = monthlyPlanItem;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public InstallmentEntryStatus getStatus() {
        return status;
    }

    public void setStatus(InstallmentEntryStatus status) {
        this.status = status;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public InstallmentPaymentSource getPaymentSource() {
        return paymentSource;
    }

    public void setPaymentSource(InstallmentPaymentSource paymentSource) {
        this.paymentSource = paymentSource;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(User paidBy) {
        this.paidBy = paidBy;
    }

    public Instant getPaymentRegisteredAt() {
        return paymentRegisteredAt;
    }

    public void setPaymentRegisteredAt(Instant paymentRegisteredAt) {
        this.paymentRegisteredAt = paymentRegisteredAt;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
