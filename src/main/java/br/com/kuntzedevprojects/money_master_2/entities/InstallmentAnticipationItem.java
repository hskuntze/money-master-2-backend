package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_installment_anticipation_item", indexes = {
        @Index(name = "idx_installment_anticipation_item_parent", columnList = "anticipation_id"),
        @Index(name = "idx_installment_anticipation_item_entry", columnList = "installment_entry_id")
})
public class InstallmentAnticipationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anticipation_id", nullable = false)
    private InstallmentAnticipation anticipation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_entry_id", nullable = false)
    private InstallmentPurchaseEntry installment;

    @Column(nullable = false)
    private LocalDate originalDueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal anticipatedAmount = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InstallmentAnticipation getAnticipation() { return anticipation; }
    public void setAnticipation(InstallmentAnticipation anticipation) { this.anticipation = anticipation; }
    public InstallmentPurchaseEntry getInstallment() { return installment; }
    public void setInstallment(InstallmentPurchaseEntry installment) { this.installment = installment; }
    public LocalDate getOriginalDueDate() { return originalDueDate; }
    public void setOriginalDueDate(LocalDate originalDueDate) { this.originalDueDate = originalDueDate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getAnticipatedAmount() { return anticipatedAmount; }
    public void setAnticipatedAmount(BigDecimal anticipatedAmount) { this.anticipatedAmount = anticipatedAmount; }
}
