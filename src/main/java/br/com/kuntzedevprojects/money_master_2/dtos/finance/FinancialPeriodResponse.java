package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;

public record FinancialPeriodResponse(
        Long id,
        String name,
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
    public static FinancialPeriodResponse from(FinancialPeriod period) {
        return new FinancialPeriodResponse(
                period.getId(),
                period.getName(),
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
}
