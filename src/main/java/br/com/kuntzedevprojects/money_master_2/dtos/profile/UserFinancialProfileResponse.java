package br.com.kuntzedevprojects.money_master_2.dtos.profile;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.UserFinancialProfile;

public record UserFinancialProfileResponse(
        Long id,
        Integer age,
        String ageRange,
        String profession,
        String preferredName,
        Integer cycleStartDay,
        Integer incomeDay,
        BigDecimal approximateMonthlyIncome,
        BigDecimal initialGoalTargetAmount,
        String onboardingVersion,
        String currentFinancialSituation,
        String spendingHabits,
        String financialObjectives,
        String shortTermGoals,
        String mediumTermGoals,
        String longTermGoals,
        String riskTolerance,
        String investmentKnowledge,
        String investorProfile,
        String financialPreferences,
        boolean onboardingCompleted,
        Instant onboardingCompletedAt,
        boolean tourCompleted,
        boolean tourSkipped,
        Instant tourCompletedAt,
        Instant tourSkippedAt,
        String tourLastStepKey,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserFinancialProfileResponse from(UserFinancialProfile profile) {
        return new UserFinancialProfileResponse(
                profile.getId(),
                profile.getAge(),
                profile.getAgeRange(),
                profile.getProfession(),
                profile.getPreferredName(),
                profile.getCycleStartDay(),
                profile.getIncomeDay(),
                profile.getApproximateMonthlyIncome(),
                profile.getInitialGoalTargetAmount(),
                profile.getOnboardingVersion(),
                profile.getCurrentFinancialSituation(),
                profile.getSpendingHabits(),
                profile.getFinancialObjectives(),
                profile.getShortTermGoals(),
                profile.getMediumTermGoals(),
                profile.getLongTermGoals(),
                profile.getRiskTolerance(),
                profile.getInvestmentKnowledge(),
                profile.getInvestorProfile(),
                profile.getFinancialPreferences(),
                profile.isOnboardingCompleted(),
                profile.getOnboardingCompletedAt(),
                profile.isTourCompleted(),
                profile.isTourSkipped(),
                profile.getTourCompletedAt(),
                profile.getTourSkippedAt(),
                profile.getTourLastStepKey(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
