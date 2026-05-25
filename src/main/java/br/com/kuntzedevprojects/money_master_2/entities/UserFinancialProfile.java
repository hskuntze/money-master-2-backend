package br.com.kuntzedevprojects.money_master_2.entities;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_user_financial_profile", indexes = {
		@Index(name = "idx_user_financial_profile_owner", columnList = "owner_id") }, uniqueConstraints = {
				@UniqueConstraint(name = "uk_user_financial_profile_owner", columnNames = "owner_id") })
public class UserFinancialProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	private Integer age;

	@Column(length = 80)
	private String ageRange;

	@Column(length = 160)
	private String profession;

	@Column(length = 80)
	private String preferredName;

	private Integer cycleStartDay;

	private Integer incomeDay;

	@Column(precision = 15, scale = 2)
	private BigDecimal approximateMonthlyIncome;

	@Column(precision = 15, scale = 2)
	private BigDecimal initialGoalTargetAmount;

	@Column(length = 40)
	private String onboardingVersion;

	@Column(length = 1000)
	private String currentFinancialSituation;

	@Column(length = 1000)
	private String spendingHabits;

	@Column(length = 1000)
	private String financialObjectives;

	@Column(length = 1000)
	private String shortTermGoals;

	@Column(length = 1000)
	private String mediumTermGoals;

	@Column(length = 1000)
	private String longTermGoals;

	@Column(length = 80)
	private String riskTolerance;

	@Column(length = 80)
	private String investmentKnowledge;

	@Column(length = 80)
	private String investorProfile;

	@Column(length = 1000)
	private String financialPreferences;

	@Column(nullable = false)
	private boolean onboardingCompleted = false;

	private Instant onboardingCompletedAt;

	@Column(nullable = false)
	private boolean tourCompleted = false;

	@Column(nullable = false)
	private boolean tourSkipped = false;

	private Instant tourCompletedAt;

	private Instant tourSkippedAt;

	@Column(length = 120)
	private String tourLastStepKey;

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

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getAgeRange() {
		return ageRange;
	}

	public void setAgeRange(String ageRange) {
		this.ageRange = ageRange;
	}

	public String getProfession() {
		return profession;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public String getPreferredName() {
		return preferredName;
	}

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}

	public Integer getCycleStartDay() {
		return cycleStartDay;
	}

	public void setCycleStartDay(Integer cycleStartDay) {
		this.cycleStartDay = cycleStartDay;
	}

	public Integer getIncomeDay() {
		return incomeDay;
	}

	public void setIncomeDay(Integer incomeDay) {
		this.incomeDay = incomeDay;
	}

	public BigDecimal getApproximateMonthlyIncome() {
		return approximateMonthlyIncome;
	}

	public void setApproximateMonthlyIncome(BigDecimal approximateMonthlyIncome) {
		this.approximateMonthlyIncome = approximateMonthlyIncome;
	}

	public BigDecimal getInitialGoalTargetAmount() {
		return initialGoalTargetAmount;
	}

	public void setInitialGoalTargetAmount(BigDecimal initialGoalTargetAmount) {
		this.initialGoalTargetAmount = initialGoalTargetAmount;
	}

	public String getOnboardingVersion() {
		return onboardingVersion;
	}

	public void setOnboardingVersion(String onboardingVersion) {
		this.onboardingVersion = onboardingVersion;
	}

	public String getCurrentFinancialSituation() {
		return currentFinancialSituation;
	}

	public void setCurrentFinancialSituation(String currentFinancialSituation) {
		this.currentFinancialSituation = currentFinancialSituation;
	}

	public String getSpendingHabits() {
		return spendingHabits;
	}

	public void setSpendingHabits(String spendingHabits) {
		this.spendingHabits = spendingHabits;
	}

	public String getFinancialObjectives() {
		return financialObjectives;
	}

	public void setFinancialObjectives(String financialObjectives) {
		this.financialObjectives = financialObjectives;
	}

	public String getShortTermGoals() {
		return shortTermGoals;
	}

	public void setShortTermGoals(String shortTermGoals) {
		this.shortTermGoals = shortTermGoals;
	}

	public String getMediumTermGoals() {
		return mediumTermGoals;
	}

	public void setMediumTermGoals(String mediumTermGoals) {
		this.mediumTermGoals = mediumTermGoals;
	}

	public String getLongTermGoals() {
		return longTermGoals;
	}

	public void setLongTermGoals(String longTermGoals) {
		this.longTermGoals = longTermGoals;
	}

	public String getRiskTolerance() {
		return riskTolerance;
	}

	public void setRiskTolerance(String riskTolerance) {
		this.riskTolerance = riskTolerance;
	}

	public String getInvestmentKnowledge() {
		return investmentKnowledge;
	}

	public void setInvestmentKnowledge(String investmentKnowledge) {
		this.investmentKnowledge = investmentKnowledge;
	}

	public String getInvestorProfile() {
		return investorProfile;
	}

	public void setInvestorProfile(String investorProfile) {
		this.investorProfile = investorProfile;
	}

	public String getFinancialPreferences() {
		return financialPreferences;
	}

	public void setFinancialPreferences(String financialPreferences) {
		this.financialPreferences = financialPreferences;
	}

	public boolean isOnboardingCompleted() {
		return onboardingCompleted;
	}

	public void setOnboardingCompleted(boolean onboardingCompleted) {
		this.onboardingCompleted = onboardingCompleted;
	}

	public Instant getOnboardingCompletedAt() {
		return onboardingCompletedAt;
	}

	public void setOnboardingCompletedAt(Instant onboardingCompletedAt) {
		this.onboardingCompletedAt = onboardingCompletedAt;
	}

	public boolean isTourCompleted() {
		return tourCompleted;
	}

	public void setTourCompleted(boolean tourCompleted) {
		this.tourCompleted = tourCompleted;
	}

	public boolean isTourSkipped() {
		return tourSkipped;
	}

	public void setTourSkipped(boolean tourSkipped) {
		this.tourSkipped = tourSkipped;
	}

	public Instant getTourCompletedAt() {
		return tourCompletedAt;
	}

	public void setTourCompletedAt(Instant tourCompletedAt) {
		this.tourCompletedAt = tourCompletedAt;
	}

	public Instant getTourSkippedAt() {
		return tourSkippedAt;
	}

	public void setTourSkippedAt(Instant tourSkippedAt) {
		this.tourSkippedAt = tourSkippedAt;
	}

	public String getTourLastStepKey() {
		return tourLastStepKey;
	}

	public void setTourLastStepKey(String tourLastStepKey) {
		this.tourLastStepKey = tourLastStepKey;
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
