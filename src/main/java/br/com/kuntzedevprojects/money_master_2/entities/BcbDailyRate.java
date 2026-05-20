package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "tb_bcb_daily_rate",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bcb_daily_rate_series_date", columnNames = {"series_code", "reference_date"})
        },
        indexes = {
                @Index(name = "idx_bcb_daily_rate_series_date", columnList = "series_code,reference_date")
        }
)
public class BcbDailyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series_code", nullable = false)
    private Integer seriesCode;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal value;

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

    public Integer getSeriesCode() {
        return seriesCode;
    }

    public void setSeriesCode(Integer seriesCode) {
        this.seriesCode = seriesCode;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
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
