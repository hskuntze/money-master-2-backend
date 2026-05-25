package br.com.kuntzedevprojects.money_master_2.dtos.onboarding;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.dtos.profile.UserFinancialProfileResponse;
import br.com.kuntzedevprojects.money_master_2.entities.UserFinancialProfile;

public record OnboardingStatusResponse(
        boolean onboardingCompleted,
        Instant onboardingCompletedAt,
        boolean tourCompleted,
        boolean tourSkipped,
        Instant tourCompletedAt,
        Instant tourSkippedAt,
        String tourLastStepKey,
        boolean shouldShowOnboarding,
        boolean shouldInviteTour,
        UserFinancialProfileResponse profile
) {
    public static OnboardingStatusResponse from(UserFinancialProfile profile) {
        boolean onboardingCompleted = profile.isOnboardingCompleted();
        boolean tourCompleted = profile.isTourCompleted();
        boolean tourSkipped = profile.isTourSkipped();
        return new OnboardingStatusResponse(
                onboardingCompleted,
                profile.getOnboardingCompletedAt(),
                tourCompleted,
                tourSkipped,
                profile.getTourCompletedAt(),
                profile.getTourSkippedAt(),
                profile.getTourLastStepKey(),
                !onboardingCompleted,
                onboardingCompleted && !tourCompleted && !tourSkipped,
                UserFinancialProfileResponse.from(profile)
        );
    }
}
