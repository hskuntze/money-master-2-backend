package br.com.kuntzedevprojects.money_master_2.services.finance.cycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

class MonthlyCycleQueryServiceTest {

    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final MonthlyCycleQueryService service = new MonthlyCycleQueryService(financialPeriodService);

    @Test
    void shouldExposeFinancialPeriodsAsMonthlyCycles() {
        when(financialPeriodService.list("ana@example.com")).thenReturn(List.of(period()));

        List<MonthlyCycleResponse> cycles = service.list("ana@example.com");

        assertAll(
                () -> assertEquals(1, cycles.size()),
                () -> assertEquals(10L, cycles.get(0).id()),
                () -> assertEquals("Junho", cycles.get(0).name()),
                () -> assertEquals(6, cycles.get(0).month()),
                () -> assertEquals(2026, cycles.get(0).year()),
                () -> assertEquals(FinancialPeriodStatus.OPEN, cycles.get(0).status())
        );
    }

    @Test
    void shouldExposeLegacySummaryAsMonthlyCycleSummary() {
        MonthlyPeriodSummaryResponse legacySummary = new MonthlyPeriodSummaryResponse(
                period(),
                money("5000.00"),
                money("2200.00"),
                money("4500.00"),
                money("1000.00"),
                money("500.00"),
                money("1200.00"),
                money("4600.00"),
                money("900.00"),
                money("2800.00"),
                money("100.00"),
                money("75.00"),
                money("2825.00"),
                3,
                4
        );
        when(financialPeriodService.summary("ana@example.com", 10L)).thenReturn(legacySummary);

        MonthlyCycleSummaryResponse summary = service.summary("ana@example.com", 10L);

        assertAll(
                () -> assertEquals(10L, summary.cycle().id()),
                () -> assertEquals(6, summary.cycle().month()),
                () -> assertEquals(money("5000.00"), summary.plannedIncomeTotal()),
                () -> assertEquals(money("2200.00"), summary.plannedExpenseTotal()),
                () -> assertEquals(money("2825.00"), summary.projectedAvailableAmount()),
                () -> assertEquals(3, summary.pendingItems()),
                () -> assertEquals(4, summary.paidItems())
        );
    }

    private FinancialPeriodResponse period() {
        return new FinancialPeriodResponse(
                10L,
                "Junho",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                1,
                FinancialPeriodStatus.OPEN,
                money("0.00"),
                money("0.00"),
                money("0.00"),
                money("0.00"),
                null,
                null,
                null
        );
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
