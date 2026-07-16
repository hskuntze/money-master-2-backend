package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.enums.DebtAmortizationMethod;
import br.com.kuntzedevprojects.money_master_2.enums.DebtStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_debt", indexes = {
        @Index(name = "idx_debt_owner_status", columnList = "owner_id,status"),
        @Index(name = "idx_debt_owner_dates", columnList = "owner_id,start_date,first_due_date")
})
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DebtType type = DebtType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DebtStatus status = DebtStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DebtAmortizationMethod amortizationMethod = DebtAmortizationMethod.CONSTANT_PRINCIPAL;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal installmentAmount;

    @Column(precision = 8, scale = 4)
    private BigDecimal annualInterestRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal annualCetRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyFeeAmount;

    @Column(nullable = false)
    private Integer installmentCount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate firstDueDate;

    @Column(nullable = false)
    private LocalDate lastDueDate;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebtInstallment> installments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant canceledAt;

    private Instant paidOffAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addInstallment(DebtInstallment installment) {
        installments.add(installment);
        installment.setDebt(this);
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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DebtType getType() {
        return type;
    }

    public void setType(DebtType type) {
        this.type = type;
    }

    public DebtStatus getStatus() {
        return status;
    }

    public void setStatus(DebtStatus status) {
        this.status = status;
    }

    public DebtAmortizationMethod getAmortizationMethod() {
        return amortizationMethod;
    }

    public void setAmortizationMethod(DebtAmortizationMethod amortizationMethod) {
        this.amortizationMethod = amortizationMethod;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public void setInstallmentAmount(BigDecimal installmentAmount) {
        this.installmentAmount = installmentAmount;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public BigDecimal getAnnualCetRate() {
        return annualCetRate;
    }

    public void setAnnualCetRate(BigDecimal annualCetRate) {
        this.annualCetRate = annualCetRate;
    }

    public BigDecimal getMonthlyFeeAmount() {
        return monthlyFeeAmount;
    }

    public void setMonthlyFeeAmount(BigDecimal monthlyFeeAmount) {
        this.monthlyFeeAmount = monthlyFeeAmount;
    }

    public Integer getInstallmentCount() {
        return installmentCount;
    }

    public void setInstallmentCount(Integer installmentCount) {
        this.installmentCount = installmentCount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getFirstDueDate() {
        return firstDueDate;
    }

    public void setFirstDueDate(LocalDate firstDueDate) {
        this.firstDueDate = firstDueDate;
    }

    public LocalDate getLastDueDate() {
        return lastDueDate;
    }

    public void setLastDueDate(LocalDate lastDueDate) {
        this.lastDueDate = lastDueDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<DebtInstallment> getInstallments() {
        return installments;
    }

    public void setInstallments(List<DebtInstallment> installments) {
        this.installments = installments;
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

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    public Instant getPaidOffAt() {
        return paidOffAt;
    }

    public void setPaidOffAt(Instant paidOffAt) {
        this.paidOffAt = paidOffAt;
    }
}
