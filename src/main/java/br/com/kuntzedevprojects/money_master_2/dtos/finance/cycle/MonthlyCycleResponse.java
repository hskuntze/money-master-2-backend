package br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;

public record MonthlyCycleResponse(
        Long id,
        String name,
        Integer month,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        Integer turnoverDay,
        FinancialPeriodStatus status,
        BigDecimal archivedIncomeTotal,
        BigDecimal archivedExpenseTotal,
        BigDecimal archivedTransferTotal,
        BigDecimal archivedNetTotal,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static MonthlyCycleResponse from(FinancialPeriod period) {
        return new MonthlyCycleResponse(
                period.getId(),
                period.getName(),
                period.getStartDate() == null ? null : period.getStartDate().getMonthValue(),
                period.getStartDate() == null ? null : period.getStartDate().getYear(),
                period.getStartDate(),
                period.getEndDate(),
                period.getTurnoverDay(),
                period.getStatus(),
                period.getArchivedIncomeTotal(),
                period.getArchivedExpenseTotal(),
                period.getArchivedTransferTotal(),
                period.getArchivedNetTotal(),
                period.getClosedAt(),
                period.getCreatedAt(),
                period.getUpdatedAt()
        );
    }

    public static MonthlyCycleResponse from(FinancialPeriodResponse period) {
        return new MonthlyCycleResponse(
                period.id(),
                period.name(),
                period.startDate() == null ? null : period.startDate().getMonthValue(),
                period.startDate() == null ? null : period.startDate().getYear(),
                period.startDate(),
                period.endDate(),
                period.turnoverDay(),
                period.status(),
                period.archivedIncomeTotal(),
                period.archivedExpenseTotal(),
                period.archivedTransferTotal(),
                period.archivedNetTotal(),
                period.closedAt(),
                period.createdAt(),
                period.updatedAt()
        );
    }
}
