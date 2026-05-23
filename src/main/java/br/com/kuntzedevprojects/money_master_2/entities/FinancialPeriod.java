package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
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
@Table(
        name = "tb_financial_period",
        indexes = {
                @Index(name = "idx_financial_period_owner_status", columnList = "owner_id,status"),
                @Index(name = "idx_financial_period_owner_dates", columnList = "owner_id,start_date,end_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_financial_period_owner_start", columnNames = {"owner_id", "start_date"})
        }
)
public class FinancialPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "turnover_day")
    private Integer turnoverDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinancialPeriodStatus status = FinancialPeriodStatus.OPEN;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal archivedIncomeTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal archivedExpenseTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal archivedTransferTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal archivedNetTotal = BigDecimal.ZERO;

    private Instant closedAt;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getTurnoverDay() {
        return turnoverDay;
    }

    public void setTurnoverDay(Integer turnoverDay) {
        this.turnoverDay = turnoverDay;
    }

    public FinancialPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FinancialPeriodStatus status) {
        this.status = status;
    }

    public BigDecimal getArchivedIncomeTotal() {
        return archivedIncomeTotal;
    }

    public void setArchivedIncomeTotal(BigDecimal archivedIncomeTotal) {
        this.archivedIncomeTotal = archivedIncomeTotal;
    }

    public BigDecimal getArchivedExpenseTotal() {
        return archivedExpenseTotal;
    }

    public void setArchivedExpenseTotal(BigDecimal archivedExpenseTotal) {
        this.archivedExpenseTotal = archivedExpenseTotal;
    }

    public BigDecimal getArchivedTransferTotal() {
        return archivedTransferTotal;
    }

    public void setArchivedTransferTotal(BigDecimal archivedTransferTotal) {
        this.archivedTransferTotal = archivedTransferTotal;
    }

    public BigDecimal getArchivedNetTotal() {
        return archivedNetTotal;
    }

    public void setArchivedNetTotal(BigDecimal archivedNetTotal) {
        this.archivedNetTotal = archivedNetTotal;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
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
