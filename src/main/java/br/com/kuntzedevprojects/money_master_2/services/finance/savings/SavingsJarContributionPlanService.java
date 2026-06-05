package br.com.kuntzedevprojects.money_master_2.services.finance.savings;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanResponse;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;

@Service
public class SavingsJarContributionPlanService {

    public static final String NOTES_PREFIX = "SAVINGS_JAR:";

    private final SavingsJarService savingsJarService;
    private final FinancialPeriodService financialPeriodService;

    public SavingsJarContributionPlanService(SavingsJarService savingsJarService, FinancialPeriodService financialPeriodService) {
        this.savingsJarService = savingsJarService;
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<SavingsJarContributionPlanResponse> list(String ownerEmail, Long cycleId) {
        List<SavingsJar> jars = savingsJarService.listEntities(ownerEmail);
        return financialPeriodService.listPlanItems(ownerEmail, cycleId, null)
                .stream()
                .filter(item -> item.type() == TransactionType.EXPENSE)
                .filter(item -> item.nature() == MonthlyPlanItemNature.SAVINGS_JAR)
                .map(item -> toResponse(jars, item))
                .toList();
    }

    @Transactional
    public SavingsJarContributionPlanResponse create(String ownerEmail, Long cycleId, Long jarId, SavingsJarContributionPlanRequest request) {
        SavingsJar jar = savingsJarService.findOwnedJar(ownerEmail, jarId);
        MonthlyPlanItemCreateRequest planRequest = new MonthlyPlanItemCreateRequest(
                jar.getLinkedAccount() == null ? null : jar.getLinkedAccount().getId(),
                null,
                TransactionType.EXPENSE,
                "Aporte " + jar.getName(),
                request.amount(),
                request.dueDate(),
                MonthlyPlanItemNature.SAVINGS_JAR,
                MonthlyPlanItemAggregationType.NORMAL,
                null,
                Boolean.TRUE.equals(request.recurring()),
                request.recurrenceEndDate(),
                MonthlyPlanItemStatus.PENDING,
                null,
                NOTES_PREFIX + jar.getId() + "\n" + normalizeNullable(request.notes())
        );
        MonthlyPlanItemResponse created = financialPeriodService.createPlanItem(ownerEmail, cycleId, planRequest);
        return new SavingsJarContributionPlanResponse(jar.getId(), jar.getName(), MonthlyPayableResponse.from(created));
    }

    private SavingsJarContributionPlanResponse toResponse(List<SavingsJar> jars, MonthlyPlanItemResponse item) {
        Long jarId = parseJarId(item.notes());
        SavingsJar jar = jars.stream().filter(candidate -> candidate.getId().equals(jarId)).findFirst().orElse(null);
        return new SavingsJarContributionPlanResponse(
                jarId,
                jar == null ? "Cofrinho" : jar.getName(),
                MonthlyPayableResponse.from(item)
        );
    }

    public static Long parseJarId(String notes) {
        if (notes == null || !notes.startsWith(NOTES_PREFIX)) {
            return null;
        }
        String raw = notes.substring(NOTES_PREFIX.length()).split("\\R", 2)[0].trim();
        if (raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
