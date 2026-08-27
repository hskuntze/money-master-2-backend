package br.com.kuntzedevprojects.money_master_2.services.finance.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPayableSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemInvoiceContributionMode;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

class InvestmentContributionPlanServiceTest {

    private final InvestmentProductService investmentProductService = mock(InvestmentProductService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final InvestmentContributionPlanService service = new InvestmentContributionPlanService(
            investmentProductService,
            financialPeriodService
    );

    @Test
    void createShouldUseInvestmentNatureAndSourceType() {
        InvestmentProduct product = product(true);
        InvestmentContributionPlanRequest request = request();
        when(investmentProductService.findOwnedProduct(product.getOwner().getEmail(), product.getId())).thenReturn(product);
        when(financialPeriodService.createPlanItem(any(String.class), any(Long.class), any(MonthlyPlanItemCreateRequest.class)))
                .thenReturn(planItemResponse(product, request));

        var response = service.create(product.getOwner().getEmail(), 20L, product.getId(), request);

        ArgumentCaptor<MonthlyPlanItemCreateRequest> planRequest = ArgumentCaptor.forClass(MonthlyPlanItemCreateRequest.class);
        verify(financialPeriodService).createPlanItem(any(String.class), any(Long.class), planRequest.capture());
        assertThat(planRequest.getValue().nature()).isEqualTo(MonthlyPlanItemNature.INVESTMENT);
        assertThat(planRequest.getValue().notes()).startsWith(InvestmentContributionPlanService.NOTES_PREFIX + product.getId());
        assertThat(response.payable().sourceType()).isEqualTo(MonthlyPayableSourceType.INVESTMENT_CONTRIBUTION);
    }

    @Test
    void createShouldRejectArchivedInvestmentProduct() {
        InvestmentProduct product = product(false);
        when(investmentProductService.findOwnedProduct(product.getOwner().getEmail(), product.getId())).thenReturn(product);

        assertThatThrownBy(() -> service.create(product.getOwner().getEmail(), 20L, product.getId(), request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("arquivado");
        verifyNoInteractions(financialPeriodService);
    }

    private InvestmentContributionPlanRequest request() {
        return new InvestmentContributionPlanRequest(
                20L,
                new BigDecimal("250.00"),
                LocalDate.of(2026, 7, 20),
                false,
                null,
                "aporte mensal"
        );
    }

    private InvestmentProduct product(boolean active) {
        User owner = new User();
        owner.setId(1L);
        owner.setName("User");
        owner.setEmail("user@example.com");

        InvestmentProduct product = new InvestmentProduct();
        product.setId(77L);
        product.setOwner(owner);
        product.setName("Capitalizacao");
        product.setTypeName("Capitalizacao");
        product.setActive(active);
        return product;
    }

    private MonthlyPlanItemResponse planItemResponse(InvestmentProduct product, InvestmentContributionPlanRequest request) {
        return new MonthlyPlanItemResponse(
                90L,
                request.cycleId(),
                "Julho",
                null,
                null,
                TransactionType.EXPENSE,
                "Aporte " + product.getName(),
                request.amount(),
                BigDecimal.ZERO,
                request.amount(),
                null,
                MonthlyPlanItemInvoiceContributionMode.COMPOSITION_ONLY,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                request.dueDate(),
                null,
                MonthlyPlanItemStatus.PENDING,
                MonthlyPlanItemNature.INVESTMENT,
                MonthlyPlanItemAggregationType.NORMAL,
                MonthlyPlanItemSettlementOrigin.DIRECT,
                false,
                true,
                false,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                request.amount(),
                List.of(),
                InvestmentContributionPlanService.NOTES_PREFIX + product.getId(),
                null,
                null
        );
    }
}
