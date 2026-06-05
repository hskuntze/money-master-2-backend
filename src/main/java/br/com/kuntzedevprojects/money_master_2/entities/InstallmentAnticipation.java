package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.enums.InstallmentAnticipationStatus;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_installment_anticipation", indexes = {
        @Index(name = "idx_installment_anticipation_owner", columnList = "owner_id"),
        @Index(name = "idx_installment_anticipation_purchase", columnList = "purchase_id"),
        @Index(name = "idx_installment_anticipation_invoice", columnList = "target_invoice_id")
})
public class InstallmentAnticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private InstallmentPurchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_invoice_id")
    private CreditCardInvoice targetInvoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_period_id", nullable = false)
    private FinancialPeriod cycle;

    @Column(nullable = false)
    private LocalDate anticipationDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal anticipatedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InstallmentAnticipationStatus status = InstallmentAnticipationStatus.ACTIVE;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "anticipation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InstallmentAnticipationItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public void addItem(InstallmentAnticipationItem item) {
        items.add(item);
        item.setAnticipation(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public InstallmentPurchase getPurchase() { return purchase; }
    public void setPurchase(InstallmentPurchase purchase) { this.purchase = purchase; }
    public CreditCard getCreditCard() { return creditCard; }
    public void setCreditCard(CreditCard creditCard) { this.creditCard = creditCard; }
    public CreditCardInvoice getTargetInvoice() { return targetInvoice; }
    public void setTargetInvoice(CreditCardInvoice targetInvoice) { this.targetInvoice = targetInvoice; }
    public FinancialPeriod getCycle() { return cycle; }
    public void setCycle(FinancialPeriod cycle) { this.cycle = cycle; }
    public LocalDate getAnticipationDate() { return anticipationDate; }
    public void setAnticipationDate(LocalDate anticipationDate) { this.anticipationDate = anticipationDate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getAnticipatedAmount() { return anticipatedAmount; }
    public void setAnticipatedAmount(BigDecimal anticipatedAmount) { this.anticipatedAmount = anticipatedAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public InstallmentAnticipationStatus getStatus() { return status; }
    public void setStatus(InstallmentAnticipationStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<InstallmentAnticipationItem> getItems() { return items; }
    public void setItems(List<InstallmentAnticipationItem> items) { this.items = items; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
