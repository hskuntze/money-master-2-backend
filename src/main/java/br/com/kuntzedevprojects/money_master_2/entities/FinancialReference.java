package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.enums.FinancialReferenceType;
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
@Table(name = "tb_financial_reference", indexes = {
		@Index(name = "idx_financial_reference_owner_active", columnList = "owner_id,active"),
		@Index(name = "idx_financial_reference_type", columnList = "type") })
public class FinancialReference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private User owner;

	@Column(nullable = false, length = 180)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FinancialReferenceType type = FinancialReferenceType.ARTICLE;

	@Column(length = 1000)
	private String url;

	@Column(length = 2000)
	private String description;

	@Column(length = 180)
	private String source;

	@Column(nullable = false)
	private boolean active = true;

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public FinancialReferenceType getType() {
		return type;
	}

	public void setType(FinancialReferenceType type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
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
