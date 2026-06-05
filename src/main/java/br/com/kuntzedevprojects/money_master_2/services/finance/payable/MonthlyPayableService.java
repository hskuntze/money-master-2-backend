package br.com.kuntzedevprojects.money_master_2.services.finance.payable;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPayableStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class MonthlyPayableService {

    private final FinancialPeriodService financialPeriodService;

    public MonthlyPayableService(FinancialPeriodService financialPeriodService) {
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<MonthlyPayableResponse> list(String ownerEmail, Long cycleId) {
        return financialPeriodService.listPlanItems(ownerEmail, cycleId, null)
                .stream()
                .filter(this::isPayable)
                .map(MonthlyPayableResponse::from)
                .toList();
    }

    @Transactional
    public MonthlyPayableResponse create(String ownerEmail, Long cycleId, MonthlyPayableCreateRequest request) {
        MonthlyPlanItemCreateRequest legacyRequest = new MonthlyPlanItemCreateRequest(
                request.accountId(),
                request.categoryId(),
                null,
                request.description(),
                request.expectedAmount(),
                request.dueDate(),
                null,
                null,
                null,
                request.recurring(),
                request.recurrenceEndDate(),
                mapStatus(request.status()),
                null,
                request.notes()
        );
        return MonthlyPayableResponse.from(financialPeriodService.createPlanItem(ownerEmail, cycleId, legacyRequest));
    }

    @Transactional
    public MonthlyPayableResponse update(String ownerEmail, Long id, MonthlyPayableUpdateRequest request) {
        ensurePayable(ownerEmail, id);
        MonthlyPlanItemUpdateRequest legacyRequest = new MonthlyPlanItemUpdateRequest(
                request.accountId(),
                request.categoryId(),
                TransactionType.EXPENSE,
                request.description(),
                request.expectedAmount(),
                null,
                request.dueDate(),
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
        return MonthlyPayableResponse.from(financialPeriodService.updatePlanItem(ownerEmail, id, legacyRequest));
    }

    @Transactional
    public void cancel(String ownerEmail, Long id) {
        ensurePayable(ownerEmail, id);
        financialPeriodService.cancelPlanItem(ownerEmail, id);
    }

    private void ensurePayable(String ownerEmail, Long id) {
        if (!isPayable(MonthlyPlanItemResponse.from(financialPeriodService.findOwnedPlanItem(ownerEmail, id)))) {
            throw new BusinessException("Conta mensal nao encontrada.");
        }
    }

    private boolean isPayable(MonthlyPlanItemResponse item) {
        return item.type() == TransactionType.EXPENSE && item.includedInMainTotals();
    }

    private MonthlyPlanItemStatus mapStatus(MonthlyPayableStatus status) {
        if (status == null || status == MonthlyPayableStatus.OVERDUE) {
            return null;
        }
        return switch (status) {
            case PENDING -> MonthlyPlanItemStatus.PENDING;
            case PARTIALLY_PAID -> MonthlyPlanItemStatus.PARTIALLY_PAID;
            case PAID -> MonthlyPlanItemStatus.PAID;
            case CANCELED -> MonthlyPlanItemStatus.CANCELED;
            case OVERDUE -> null;
        };
    }
}
