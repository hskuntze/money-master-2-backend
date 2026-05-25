package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus;
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
@Table(name = "tb_installment_purchase", indexes = {
		@Index(name = "idx_installment_purchase_owner_status", columnList = "owner_id,status"),
		@Index(name = "idx_installment_purchase_owner_dates", columnList = "owner_id,first_due_date,last_due_date") })
public class InstallmentPurchase {

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
	private String description;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(nullable = false)
	private Integer installmentCount;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal installmentAmount = BigDecimal.ZERO;

	@Column(nullable = false)
	private LocalDate purchaseDate;

	@Column(nullable = false)
	private LocalDate firstDueDate;

	@Column(nullable = false)
	private LocalDate lastDueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private InstallmentPurchaseStatus status = InstallmentPurchaseStatus.ACTIVE;

	@Column(length = 2000)
	private String notes;

	@OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InstallmentPurchaseEntry> entries = new ArrayList<>();

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

	public void addEntry(InstallmentPurchaseEntry entry) {
		entries.add(entry);
		entry.setPurchase(this);
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public Integer getInstallmentCount() {
		return installmentCount;
	}

	public void setInstallmentCount(Integer installmentCount) {
		this.installmentCount = installmentCount;
	}

	public BigDecimal getInstallmentAmount() {
		return installmentAmount;
	}

	public void setInstallmentAmount(BigDecimal installmentAmount) {
		this.installmentAmount = installmentAmount;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
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

	public InstallmentPurchaseStatus getStatus() {
		return status;
	}

	public void setStatus(InstallmentPurchaseStatus status) {
		this.status = status;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public List<InstallmentPurchaseEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<InstallmentPurchaseEntry> entries) {
		this.entries = entries;
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
