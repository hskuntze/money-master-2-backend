package br.com.kuntzedevprojects.money_master_2.services.finance.planning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyIncomePlanStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPayableSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPayableStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

class MonthlyPlanningAdapterTest {

    @Test
    void shouldExposeIncomePlanSemanticsFromLegacyPlanItem() {
        MonthlyIncomePlanResponse income = MonthlyIncomePlanResponse.from(planItem(
                TransactionType.INCOME,
                "Salario",
                money("5000.00"),
                money("1200.00"),
                LocalDate.of(2026, 6, 5),
                MonthlyPlanItemStatus.PARTIALLY_PAID,
                MonthlyPlanItemNature.VARIABLE,
                MonthlyPlanItemAggregationType.NORMAL
        ));

        assertAll(
                () -> assertEquals(10L, income.id()),
                () -> assertEquals(10L, income.legacyPlanItemId()),
                () -> assertEquals(1L, income.cycleId()),
                () -> assertEquals("Salario", income.description()),
                () -> assertEquals(MonthlyIncomePlanStatus.PARTIALLY_RECEIVED, income.status()),
                () -> assertEquals(money("3800.00"), income.pendingAmount())
        );
    }

    @Test
    void shouldExposeOverduePayableSemanticsFromLegacyPlanItem() {
        MonthlyPayableResponse payable = MonthlyPayableResponse.from(planItem(
                TransactionType.EXPENSE,
                "Aluguel",
                money("1800.00"),
                money("0.00"),
                LocalDate.now().minusDays(1),
                MonthlyPlanItemStatus.PENDING,
                MonthlyPlanItemNature.FIXED,
                MonthlyPlanItemAggregationType.NORMAL
        ));

        assertAll(
                () -> assertEquals(MonthlyPayableStatus.OVERDUE, payable.status()),
                () -> assertEquals(MonthlyPayableSourceType.LEGACY_PLAN_ITEM, payable.sourceType()),
                () -> assertEquals(money("1800.00"), payable.pendingAmount())
        );
    }

    @Test
    void shouldExposeInvoiceAsCreditCardInvoicePayable() {
        MonthlyPayableResponse payable = MonthlyPayableResponse.from(planItem(
                TransactionType.EXPENSE,
                "Fatura Nubank",
                money("700.00"),
                money("700.00"),
                LocalDate.now(),
                MonthlyPlanItemStatus.PAID,
                MonthlyPlanItemNature.CREDIT_CARD,
                MonthlyPlanItemAggregationType.GROUP_PARENT
        ));

        assertAll(
                () -> assertEquals(MonthlyPayableStatus.PAID, payable.status()),
                () -> assertEquals(MonthlyPayableSourceType.CREDIT_CARD_INVOICE, payable.sourceType()),
                () -> assertEquals(money("0.00"), payable.pendingAmount())
        );
    }

    private MonthlyPlanItemResponse planItem(
            TransactionType type,
            String description,
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            LocalDate dueDate,
            MonthlyPlanItemStatus status,
            MonthlyPlanItemNature nature,
            MonthlyPlanItemAggregationType aggregationType
    ) {
        return new MonthlyPlanItemResponse(
                10L,
                1L,
                "Junho",
                null,
                null,
                type,
                description,
                expectedAmount,
                actualAmount,
                expectedAmount.subtract(actualAmount).max(BigDecimal.ZERO),
                null,
                null,
                money("0.00"),
                money("0.00"),
                dueDate,
                status == MonthlyPlanItemStatus.PAID ? dueDate : null,
                status,
                nature,
                aggregationType,
                MonthlyPlanItemSettlementOrigin.DIRECT,
                false,
                aggregationType != MonthlyPlanItemAggregationType.GROUP_CHILD,
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
                money("0.00"),
                money("0.00"),
                money("0.00"),
                List.of(),
                null,
                null,
                null
        );
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
