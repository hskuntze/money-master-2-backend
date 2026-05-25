package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.OnboardingFixedBillRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.OnboardingSetupRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.OnboardingStatusResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.onboarding.TourStateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.entities.UserFinancialProfile;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialPeriodRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserFinancialProfileRepository;

@Service
public class UserOnboardingService {

    private static final String ONBOARDING_VERSION = "2026-05-monthly-planning";
    private static final DateTimeFormatter PERIOD_NAME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserFinancialProfileRepository profileRepository;
    private final CurrentUserService currentUserService;
    private final FinancialPeriodRepository periodRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final AccountService accountService;

    public UserOnboardingService(
            UserFinancialProfileRepository profileRepository,
            CurrentUserService currentUserService,
            FinancialPeriodRepository periodRepository,
            MonthlyPlanItemRepository planItemRepository,
            AccountService accountService
    ) {
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
        this.periodRepository = periodRepository;
        this.planItemRepository = planItemRepository;
        this.accountService = accountService;
    }

    @Transactional
    public OnboardingStatusResponse status(String ownerEmail) {
        return OnboardingStatusResponse.from(resolveOrCreate(ownerEmail));
    }

    @Transactional
    public OnboardingStatusResponse complete(String ownerEmail, OnboardingSetupRequest request) {
        if (request == null) {
            throw new BusinessException("Informe os dados da configuração inicial.");
        }

        User owner = currentUserService.findUserByEmail(ownerEmail);
        UserFinancialProfile profile = resolveOrCreate(ownerEmail);
        profile.setPreferredName(normalize(request.preferredName(), 80));
        profile.setAge(request.age());
        profile.setProfession(normalize(request.profession(), 160));
        profile.setCycleStartDay(request.cycleStartDay());
        profile.setIncomeDay(request.incomeDay());
        profile.setApproximateMonthlyIncome(nullToZero(request.monthlyIncome()));
        profile.setFinancialObjectives(joinGoals(request.goals()));
        profile.setInitialGoalTargetAmount(request.initialGoalTargetAmount() == null ? null : nullToZero(request.initialGoalTargetAmount()));
        profile.setInvestmentKnowledge(normalize(request.investmentKnowledge(), 80));
        profile.setRiskTolerance(normalize(request.riskTolerance(), 80));
        profile.setInvestorProfile(normalize(request.investorProfile(), 80));
        profile.setOnboardingVersion(ONBOARDING_VERSION);
        profile.setOnboardingCompleted(true);
        if (profile.getOnboardingCompletedAt() == null) {
            profile.setOnboardingCompletedAt(Instant.now());
        }

        FinancialPeriod period = resolveCurrentPlanningPeriod(ownerEmail, owner, request.cycleStartDay());
        Account account = accountService.getOrCreateDefaultAccount(ownerEmail);
        createIncomePlanItemIfNeeded(owner, account, period, request.monthlyIncome(), request.incomeDay());
        createFixedBillItems(owner, account, period, request.fixedBills());

        if (Boolean.TRUE.equals(request.startTourAfterOnboarding())) {
            profile.setTourSkipped(false);
            profile.setTourCompleted(false);
            profile.setTourSkippedAt(null);
            profile.setTourCompletedAt(null);
            profile.setTourLastStepKey(null);
        } else {
            profile.setTourSkipped(true);
            if (profile.getTourSkippedAt() == null) {
                profile.setTourSkippedAt(Instant.now());
            }
        }

        return OnboardingStatusResponse.from(profile);
    }

    @Transactional
    public OnboardingStatusResponse updateTourState(String ownerEmail, TourStateRequest request) {
        UserFinancialProfile profile = resolveOrCreate(ownerEmail);
        String action = request == null || request.action() == null ? "PROGRESS" : request.action().trim().toUpperCase(Locale.ROOT);
        Instant now = Instant.now();
        if (request != null && request.lastStepKey() != null) {
            profile.setTourLastStepKey(normalize(request.lastStepKey(), 120));
        }

        switch (action) {
            case "COMPLETE" -> {
                profile.setTourCompleted(true);
                profile.setTourSkipped(false);
                profile.setTourCompletedAt(now);
                profile.setTourSkippedAt(null);
            }
            case "SKIP" -> {
                profile.setTourSkipped(true);
                profile.setTourSkippedAt(now);
            }
            case "RESET" -> {
                profile.setTourCompleted(false);
                profile.setTourSkipped(false);
                profile.setTourCompletedAt(null);
                profile.setTourSkippedAt(null);
                profile.setTourLastStepKey(null);
            }
            case "PROGRESS" -> {
                // O passo atual já foi salvo acima.
            }
            default -> throw new BusinessException("A ação do tour deve ser COMPLETE, SKIP, RESET ou PROGRESS.");
        }
        return OnboardingStatusResponse.from(profile);
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

    private FinancialPeriod resolveCurrentPlanningPeriod(String ownerEmail, User owner, int cycleStartDay) {
        LocalDate today = LocalDate.now();
        LocalDate start = calculateCycleStart(today, cycleStartDay);
        LocalDate end = calculateCycleEnd(start, cycleStartDay);

        return periodRepository.findPeriodContainingDate(ownerEmail, today)
                .map(period -> {
                    period.setTurnoverDay(cycleStartDay);
                    return period;
                })
                .orElseGet(() -> {
                    FinancialPeriod period = new FinancialPeriod();
                    period.setOwner(owner);
                    period.setName("Ciclo iniciado em " + start.format(PERIOD_NAME_FORMATTER));
                    period.setStartDate(start);
                    period.setEndDate(end);
                    period.setTurnoverDay(cycleStartDay);
                    period.setStatus(start.isAfter(today) ? FinancialPeriodStatus.SCHEDULED : FinancialPeriodStatus.OPEN);
                    return periodRepository.save(period);
                });
    }

    private void createIncomePlanItemIfNeeded(User owner, Account account, FinancialPeriod period, BigDecimal monthlyIncome, Integer incomeDay) {
        BigDecimal amount = nullToZero(monthlyIncome);
        if (amount.signum() <= 0) {
            return;
        }
        boolean exists = planItemRepository.findActiveCandidatesByOwnerEmailAndPeriod(owner.getEmail(), period.getId(), TransactionType.INCOME)
                .stream()
                .anyMatch(item -> normalizeComparable(item.getDescription()).equals("renda principal"));
        if (exists) {
            return;
        }
        MonthlyPlanItem item = baseItem(owner, account, period, TransactionType.INCOME, "Renda principal", amount, incomeDay);
        item.setNotes("Criado automaticamente pela configuração inicial.");
        planItemRepository.save(item);
    }

    private void createFixedBillItems(User owner, Account account, FinancialPeriod period, List<OnboardingFixedBillRequest> fixedBills) {
        if (fixedBills == null || fixedBills.isEmpty()) {
            return;
        }
        fixedBills.stream()
                .filter(bill -> bill != null && bill.description() != null && !bill.description().isBlank())
                .filter(bill -> bill.amount() != null && bill.amount().signum() > 0)
                .forEach(bill -> {
                    String description = normalizeRequired(bill.description(), "Informe o nome da conta fixa.");
                    boolean exists = planItemRepository.findActiveCandidatesByOwnerEmailAndPeriod(owner.getEmail(), period.getId(), TransactionType.EXPENSE)
                            .stream()
                            .anyMatch(item -> normalizeComparable(item.getDescription()).equals(normalizeComparable(description)));
                    if (exists) {
                        return;
                    }
                    MonthlyPlanItem item = baseItem(owner, account, period, TransactionType.EXPENSE, description, bill.amount(), bill.dueDay());
                    item.setNotes("Conta fixa criada automaticamente pela configuração inicial.");
                    planItemRepository.save(item);
                });
    }

    private MonthlyPlanItem baseItem(User owner, Account account, FinancialPeriod period, TransactionType type, String description, BigDecimal amount, Integer day) {
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setOwner(owner);
        item.setFinancialPeriod(period);
        item.setAccount(account);
        item.setCategory(null);
        item.setType(type);
        item.setDescription(description);
        item.setExpectedAmount(nullToZero(amount));
        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setDueDate(dateInsidePeriod(period, day == null ? period.getStartDate().getDayOfMonth() : day));
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(MonthlyPlanItemNature.FIXED);
        item.setRecurring(true);
        return item;
    }

    private LocalDate calculateCycleStart(LocalDate reference, int cycleStartDay) {
        int day = clampDay(cycleStartDay, YearMonth.from(reference));
        LocalDate candidate = reference.withDayOfMonth(day);
        if (candidate.isAfter(reference)) {
            YearMonth previous = YearMonth.from(reference).minusMonths(1);
            return previous.atDay(clampDay(cycleStartDay, previous));
        }
        return candidate;
    }

    private LocalDate calculateCycleEnd(LocalDate start, int cycleStartDay) {
        YearMonth next = YearMonth.from(start).plusMonths(1);
        return next.atDay(clampDay(cycleStartDay, next)).minusDays(1);
    }

    private LocalDate dateInsidePeriod(FinancialPeriod period, int requestedDay) {
        LocalDate start = period.getStartDate();
        LocalDate candidate = start.withDayOfMonth(Math.min(requestedDay, start.lengthOfMonth()));
        if (candidate.isBefore(period.getStartDate())) {
            LocalDate next = start.plusMonths(1);
            candidate = next.withDayOfMonth(Math.min(requestedDay, next.lengthOfMonth()));
        }
        if (candidate.isAfter(period.getEndDate())) {
            return period.getEndDate();
        }
        return candidate;
    }

    private int clampDay(int day, YearMonth yearMonth) {
        return Math.max(1, Math.min(day, yearMonth.lengthOfMonth()));
    }

    private String joinGoals(List<String> goals) {
        if (goals == null || goals.isEmpty()) {
            return null;
        }
        String value = goals.stream()
                .filter(goal -> goal != null && !goal.isBlank())
                .map(goal -> normalize(goal, 80))
                .collect(Collectors.joining(", "));
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return normalize(value, 255);
    }

    private String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private String normalizeComparable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
