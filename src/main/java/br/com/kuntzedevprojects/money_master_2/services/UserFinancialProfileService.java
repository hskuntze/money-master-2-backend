package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.profile.UserFinancialProfileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.profile.UserFinancialProfileResponse;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.entities.UserFinancialProfile;
import br.com.kuntzedevprojects.money_master_2.repositories.UserFinancialProfileRepository;

@Service
public class UserFinancialProfileService {

    private final UserFinancialProfileRepository profileRepository;
    private final CurrentUserService currentUserService;

    public UserFinancialProfileService(UserFinancialProfileRepository profileRepository, CurrentUserService currentUserService) {
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public UserFinancialProfileResponse getOrCreate(String ownerEmail) {
        return UserFinancialProfileResponse.from(resolveOrCreate(ownerEmail));
    }

    @Transactional
    public UserFinancialProfileResponse update(String ownerEmail, UserFinancialProfileRequest request) {
        UserFinancialProfile profile = resolveOrCreate(ownerEmail);
        profile.setAge(request.age());
        profile.setAgeRange(normalize(request.ageRange(), 80));
        profile.setProfession(normalize(request.profession(), 160));
        profile.setPreferredName(normalize(request.preferredName(), 80));
        profile.setCycleStartDay(request.cycleStartDay());
        profile.setIncomeDay(request.incomeDay());
        profile.setApproximateMonthlyIncome(request.approximateMonthlyIncome());
        profile.setInitialGoalTargetAmount(request.initialGoalTargetAmount());
        profile.setOnboardingVersion(normalize(request.onboardingVersion(), 40));
        profile.setCurrentFinancialSituation(normalize(request.currentFinancialSituation(), 1000));
        profile.setSpendingHabits(normalize(request.spendingHabits(), 1000));
        profile.setFinancialObjectives(normalize(request.financialObjectives(), 1000));
        profile.setShortTermGoals(normalize(request.shortTermGoals(), 1000));
        profile.setMediumTermGoals(normalize(request.mediumTermGoals(), 1000));
        profile.setLongTermGoals(normalize(request.longTermGoals(), 1000));
        profile.setRiskTolerance(normalize(request.riskTolerance(), 80));
        profile.setInvestmentKnowledge(normalize(request.investmentKnowledge(), 80));
        profile.setInvestorProfile(normalize(request.investorProfile(), 80));
        profile.setFinancialPreferences(normalize(request.financialPreferences(), 1000));
        if (Boolean.TRUE.equals(request.onboardingCompleted())) {
            profile.setOnboardingCompleted(true);
            if (profile.getOnboardingCompletedAt() == null) {
                profile.setOnboardingCompletedAt(Instant.now());
            }
        }
        if (Boolean.TRUE.equals(request.tourCompleted())) {
            profile.setTourCompleted(true);
            profile.setTourSkipped(false);
            if (profile.getTourCompletedAt() == null) {
                profile.setTourCompletedAt(Instant.now());
            }
        }
        if (Boolean.TRUE.equals(request.tourSkipped())) {
            profile.setTourSkipped(true);
            if (profile.getTourSkippedAt() == null) {
                profile.setTourSkippedAt(Instant.now());
            }
        }
        if (request.tourLastStepKey() != null) {
            profile.setTourLastStepKey(normalize(request.tourLastStepKey(), 120));
        }
        return UserFinancialProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public String buildPromptContext(String ownerEmail) {
        return profileRepository.findByOwnerEmailIgnoreCase(ownerEmail)
                .map(this::toPromptContext)
                .orElse("Perfil financeiro ainda não preenchido. Faça perguntas de diagnóstico antes de personalizar recomendações.");
    }

    private UserFinancialProfile resolveOrCreate(String ownerEmail) {
        return profileRepository.findByOwnerEmailIgnoreCase(ownerEmail)
                .orElseGet(() -> {
                    User owner = currentUserService.findUserByEmail(ownerEmail);
                    UserFinancialProfile profile = new UserFinancialProfile();
                    profile.setOwner(owner);
                    return profileRepository.save(profile);
                });
    }

    private String toPromptContext(UserFinancialProfile profile) {
        List<String> parts = new ArrayList<>();
        add(parts, "apelido", profile.getPreferredName());
        add(parts, "idade", profile.getAge() == null ? profile.getAgeRange() : String.valueOf(profile.getAge()));
        add(parts, "profissão", profile.getProfession());
        add(parts, "dia de início do ciclo mensal", profile.getCycleStartDay() == null ? null : String.valueOf(profile.getCycleStartDay()));
        add(parts, "dia de recebimento da renda", profile.getIncomeDay() == null ? null : String.valueOf(profile.getIncomeDay()));
        add(parts, "renda mensal aproximada", profile.getApproximateMonthlyIncome() == null ? null : "R$ " + profile.getApproximateMonthlyIncome());
        add(parts, "meta financeira inicial", profile.getInitialGoalTargetAmount() == null ? null : "R$ " + profile.getInitialGoalTargetAmount());
        add(parts, "situação financeira", profile.getCurrentFinancialSituation());
        add(parts, "hábitos de consumo", profile.getSpendingHabits());
        add(parts, "objetivos financeiros", profile.getFinancialObjectives());
        add(parts, "metas de curto prazo", profile.getShortTermGoals());
        add(parts, "metas de médio prazo", profile.getMediumTermGoals());
        add(parts, "metas de longo prazo", profile.getLongTermGoals());
        add(parts, "tolerância ao risco", profile.getRiskTolerance());
        add(parts, "conhecimento em investimentos", profile.getInvestmentKnowledge());
        add(parts, "perfil de investidor", profile.getInvestorProfile());
        add(parts, "preferências", profile.getFinancialPreferences());
        if (parts.isEmpty()) {
            return "Perfil financeiro criado, mas ainda sem detalhes suficientes. Faça perguntas de diagnóstico antes de personalizar recomendações.";
        }
        return String.join("; ", parts) + ".";
    }

    private void add(List<String> parts, String label, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(label + ": " + value.trim());
        }
    }

    private String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
