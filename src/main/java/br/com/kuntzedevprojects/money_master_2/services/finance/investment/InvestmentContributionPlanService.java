package br.com.kuntzedevprojects.money_master_2.services.finance.investment;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentContributionPlanResponse;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class InvestmentContributionPlanService {

    public static final String NOTES_PREFIX = "INVESTMENT:";

    private final InvestmentProductService investmentProductService;
    private final FinancialPeriodService financialPeriodService;

    public InvestmentContributionPlanService(
            InvestmentProductService investmentProductService,
            FinancialPeriodService financialPeriodService
    ) {
        this.investmentProductService = investmentProductService;
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<InvestmentContributionPlanResponse> list(String ownerEmail, Long cycleId) {
        List<InvestmentProduct> products = investmentProductService.listEntities(ownerEmail);
        return financialPeriodService.listPlanItems(ownerEmail, cycleId, null)
                .stream()
                .filter(item -> item.type() == TransactionType.EXPENSE)
                .filter(item -> item.nature() == MonthlyPlanItemNature.INVESTMENT)
                .map(item -> toResponse(products, item))
                .toList();
    }

    @Transactional
    public InvestmentContributionPlanResponse create(String ownerEmail, Long cycleId, Long productId, InvestmentContributionPlanRequest request) {
        InvestmentProduct product = investmentProductService.findOwnedProduct(ownerEmail, productId);
        if (!product.isActive()) {
            throw new BusinessException("Este produto financeiro esta arquivado. Reative-o antes de planejar novos aportes.");
        }
        MonthlyPlanItemCreateRequest planRequest = new MonthlyPlanItemCreateRequest(
                product.getLinkedAccount() == null ? null : product.getLinkedAccount().getId(),
                null,
                TransactionType.EXPENSE,
                "Aporte " + product.getName(),
                request.amount(),
                request.dueDate(),
                MonthlyPlanItemNature.INVESTMENT,
                MonthlyPlanItemAggregationType.NORMAL,
                null,
                Boolean.TRUE.equals(request.recurring()),
                request.recurrenceEndDate(),
                MonthlyPlanItemStatus.PENDING,
                null,
                NOTES_PREFIX + product.getId() + "\n" + normalizeNullable(request.notes())
        );
        MonthlyPlanItemResponse created = financialPeriodService.createPlanItem(ownerEmail, cycleId, planRequest);
        return new InvestmentContributionPlanResponse(product.getId(), product.getName(), MonthlyPayableResponse.from(created));
    }

    public static Long parseInvestmentProductId(String notes) {
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

    private InvestmentContributionPlanResponse toResponse(List<InvestmentProduct> products, MonthlyPlanItemResponse item) {
        Long productId = parseInvestmentProductId(item.notes());
        InvestmentProduct product = products.stream()
                .filter(candidate -> candidate.getId().equals(productId))
                .findFirst()
                .orElse(null);
        return new InvestmentContributionPlanResponse(
                productId,
                product == null ? "Produto financeiro" : product.getName(),
                MonthlyPayableResponse.from(item)
        );
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
