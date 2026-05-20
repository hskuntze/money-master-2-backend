package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarYieldCalculationType;

public record SavingsJarResponse(
        Long id,
        String name,
        String institutionName,
        String description,
        BigDecimal targetAmount,
        LocalDate targetDate,
        String imageUrl,
        String icon,
        String color,
        Long linkedAccountId,
        String linkedAccountName,
        boolean active,
        boolean yieldEnabled,
        SavingsJarYieldCalculationType yieldCalculationType,
        BigDecimal yieldPercentage,
        boolean businessDaysOnly,
        boolean useBrazilianHolidays,
        LocalDate yieldStartDate,
        LocalDate lastYieldCalculationDate,
        BigDecimal currentAmount,
        BigDecimal totalYield,
        BigDecimal principalAmount,
        BigDecimal remainingToTarget,
        BigDecimal progressPercentage,
        SavingsJarYieldPreviewResponse projectedYieldToday,
        Instant createdAt,
        Instant updatedAt
) {
    public static SavingsJarResponse from(
            SavingsJar jar,
            BigDecimal currentAmount,
            BigDecimal totalYield,
            SavingsJarYieldPreviewResponse projectedYieldToday
    ) {
        BigDecimal target = nullToZero(jar.getTargetAmount());
        BigDecimal current = nullToZero(currentAmount);
        BigDecimal yield = nullToZero(totalYield);
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal progress = BigDecimal.ZERO;
        if (target.signum() > 0) {
            progress = current
                    .multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100));
        }

        return new SavingsJarResponse(
                jar.getId(),
                jar.getName(),
                jar.getInstitutionName(),
                jar.getDescription(),
                target,
                jar.getTargetDate(),
                jar.getImageUrl(),
                jar.getIcon(),
                jar.getColor(),
                jar.getLinkedAccount() == null ? null : jar.getLinkedAccount().getId(),
                jar.getLinkedAccount() == null ? null : jar.getLinkedAccount().getName(),
                jar.isActive(),
                jar.isYieldEnabled(),
                jar.getYieldCalculationType(),
                jar.getYieldPercentage(),
                jar.isBusinessDaysOnly(),
                jar.isUseBrazilianHolidays(),
                jar.getYieldStartDate(),
                jar.getLastYieldCalculationDate(),
                current.setScale(2, RoundingMode.HALF_UP),
                yield.setScale(2, RoundingMode.HALF_UP),
                current.subtract(yield).setScale(2, RoundingMode.HALF_UP),
                remaining,
                progress,
                projectedYieldToday,
                jar.getCreatedAt(),
                jar.getUpdatedAt()
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
