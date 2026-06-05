package br.com.kuntzedevprojects.money_master_2.services.finance.income;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyIncomePlanStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class MonthlyIncomePlanService {

    private final FinancialPeriodService financialPeriodService;

    public MonthlyIncomePlanService(FinancialPeriodService financialPeriodService) {
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<MonthlyIncomePlanResponse> list(String ownerEmail, Long cycleId) {
        return financialPeriodService.listPlanItems(ownerEmail, cycleId, null)
                .stream()
                .filter(this::isIncomePlan)
                .map(MonthlyIncomePlanResponse::from)
                .toList();
    }

    @Transactional
    public MonthlyIncomePlanResponse create(String ownerEmail, Long cycleId, MonthlyIncomePlanCreateRequest request) {
        MonthlyPlanItemCreateRequest legacyRequest = new MonthlyPlanItemCreateRequest(
                request.accountId(),
                request.categoryId(),
                null,
                request.description(),
                request.expectedAmount(),
                request.expectedDate(),
                null,
                null,
                null,
                request.recurring(),
                request.recurrenceEndDate(),
                mapStatus(request.status()),
                null,
                request.notes()
        );
        return MonthlyIncomePlanResponse.from(financialPeriodService.createPlanItem(ownerEmail, cycleId, legacyRequest));
    }

    @Transactional
    public MonthlyIncomePlanResponse update(String ownerEmail, Long id, MonthlyIncomePlanUpdateRequest request) {
        ensureIncome(ownerEmail, id);
        MonthlyPlanItemUpdateRequest legacyRequest = new MonthlyPlanItemUpdateRequest(
                request.accountId(),
                request.categoryId(),
                TransactionType.INCOME,
                request.description(),
                request.expectedAmount(),
                null,
                request.expectedDate(),
                null,
                MonthlyPlanItemNature.VARIABLE,
                MonthlyPlanItemAggregationType.NORMAL,
                null,
                request.recurring(),
                request.recurrenceEndDate(),
                mapStatus(request.status()),
                null,
                request.notes()
        );
        return MonthlyIncomePlanResponse.from(financialPeriodService.updatePlanItem(ownerEmail, id, legacyRequest));
    }

    @Transactional
    public void cancel(String ownerEmail, Long id) {
        ensureIncome(ownerEmail, id);
        financialPeriodService.cancelPlanItem(ownerEmail, id);
    }

    private void ensureIncome(String ownerEmail, Long id) {
        if (!isIncomePlan(MonthlyPlanItemResponse.from(financialPeriodService.findOwnedPlanItem(ownerEmail, id)))) {
            throw new BusinessException("Receita mensal nao encontrada.");
        }
    }

    private boolean isIncomePlan(MonthlyPlanItemResponse item) {
        return item.type() == TransactionType.INCOME && item.includedInMainTotals();
    }

    private MonthlyPlanItemStatus mapStatus(MonthlyIncomePlanStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case EXPECTED -> MonthlyPlanItemStatus.PENDING;
            case PARTIALLY_RECEIVED -> MonthlyPlanItemStatus.PARTIALLY_PAID;
            case RECEIVED -> MonthlyPlanItemStatus.PAID;
            case CANCELED -> MonthlyPlanItemStatus.CANCELED;
        };
    }
}
