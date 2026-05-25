package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
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
        name = "tb_monthly_plan_item",
        indexes = {
                @Index(name = "idx_monthly_plan_item_owner_period", columnList = "owner_id,financial_period_id"),
                @Index(name = "idx_monthly_plan_item_status", columnList = "status"),
                @Index(name = "idx_monthly_plan_item_due_date", columnList = "due_date"),
                @Index(name = "idx_monthly_plan_item_parent", columnList = "parent_item_id"),
                @Index(name = "idx_monthly_plan_item_aggregation", columnList = "aggregation_type"),
                @Index(name = "idx_monthly_plan_item_rec_template", columnList = "recurring_template_id"),
                @Index(name = "idx_monthly_plan_item_rec_key", columnList = "recurrence_key")
        }
)
public class MonthlyPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_period_id", nullable = false)
    private FinancialPeriod financialPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id")
    private MonthlyPlanItem parentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MonthlyPlanItemStatus status = MonthlyPlanItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MonthlyPlanItemNature nature = MonthlyPlanItemNature.VARIABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", nullable = false, length = 30)
    private MonthlyPlanItemAggregationType aggregationType = MonthlyPlanItemAggregationType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_origin", nullable = false, length = 30)
    private MonthlyPlanItemSettlementOrigin settlementOrigin = MonthlyPlanItemSettlementOrigin.DIRECT;

    @Column(name = "paid_by_parent", nullable = false)
    private boolean paidByParent = false;

    @Column(nullable = false)
    private boolean recurring = false;

    /**
     * Identificador do item que originou a série recorrente. Para o item modelo,
     * este campo aponta para o próprio id depois da primeira gravação.
     */
    private Long recurringTemplateId;

    /**
     * Item imediatamente usado como base para gerar esta ocorrência.
     */
    private Long generatedFromItemId;

    /**
     * Chave idempotente por usuário/modelo/ciclo. Evita duplicidade mesmo que a
     * rotina de recorrência seja executada várias vezes.
     */
    @Column(length = 120)
    private String recurrenceKey;

    @Column(nullable = false)
    private boolean recurrenceModifiedManually = false;

    /**
     * Data limite para repetição automática do item. Quando preenchida,
     * a clonagem para ciclos futuros para após esta data.
     */
    private LocalDate recurrenceEndDate;

    @Column(length = 2000)
    private String notes;

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

    public FinancialPeriod getFinancialPeriod() {
        return financialPeriod;
    }

    public void setFinancialPeriod(FinancialPeriod financialPeriod) {
        this.financialPeriod = financialPeriod;
    }

    public MonthlyPlanItem getParentItem() {
        return parentItem;
    }

    public void setParentItem(MonthlyPlanItem parentItem) {
        this.parentItem = parentItem;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public MonthlyPlanItemStatus getStatus() {
        return status;
    }

    public void setStatus(MonthlyPlanItemStatus status) {
        this.status = status;
    }

    public MonthlyPlanItemNature getNature() {
        return nature;
    }

    public void setNature(MonthlyPlanItemNature nature) {
        this.nature = nature;
    }

    public MonthlyPlanItemAggregationType getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(MonthlyPlanItemAggregationType aggregationType) {
        this.aggregationType = aggregationType;
    }

    public MonthlyPlanItemSettlementOrigin getSettlementOrigin() {
        return settlementOrigin;
    }

    public void setSettlementOrigin(MonthlyPlanItemSettlementOrigin settlementOrigin) {
        this.settlementOrigin = settlementOrigin;
    }

    public boolean isPaidByParent() {
        return paidByParent;
    }

    public void setPaidByParent(boolean paidByParent) {
        this.paidByParent = paidByParent;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public Long getRecurringTemplateId() {
        return recurringTemplateId;
    }

    public void setRecurringTemplateId(Long recurringTemplateId) {
        this.recurringTemplateId = recurringTemplateId;
    }

    public Long getGeneratedFromItemId() {
        return generatedFromItemId;
    }

    public void setGeneratedFromItemId(Long generatedFromItemId) {
        this.generatedFromItemId = generatedFromItemId;
    }

    public String getRecurrenceKey() {
        return recurrenceKey;
    }

    public void setRecurrenceKey(String recurrenceKey) {
        this.recurrenceKey = recurrenceKey;
    }

    public boolean isRecurrenceModifiedManually() {
        return recurrenceModifiedManually;
    }

    public void setRecurrenceModifiedManually(boolean recurrenceModifiedManually) {
        this.recurrenceModifiedManually = recurrenceModifiedManually;
    }

    public LocalDate getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
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
