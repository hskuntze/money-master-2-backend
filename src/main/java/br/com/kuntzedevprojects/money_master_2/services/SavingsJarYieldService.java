package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarApplyYieldResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarYieldPreviewResponse;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJarMovement;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarYieldCalculationType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarMovementRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarRepository;

@Service
public class SavingsJarYieldService {

    private final SavingsJarRepository savingsJarRepository;
    private final SavingsJarMovementRepository movementRepository;
    private final BcbSgsService bcbSgsService;

    public SavingsJarYieldService(
            SavingsJarRepository savingsJarRepository,
            SavingsJarMovementRepository movementRepository,
            BcbSgsService bcbSgsService
    ) {
        this.savingsJarRepository = savingsJarRepository;
        this.movementRepository = movementRepository;
        this.bcbSgsService = bcbSgsService;
    }

    @Transactional
    public List<SavingsJarApplyYieldResponse> applyPendingYieldsForAll(LocalDate to) {
        return savingsJarRepository.findActiveCdiYieldEnabled()
                .stream()
                .map(jar -> applyPendingYields(jar, to))
                .toList();
    }

    @Transactional
    public SavingsJarApplyYieldResponse applyPendingYields(SavingsJar jar, LocalDate to) {
        LocalDate targetDate = to == null ? LocalDate.now() : to;

        if (!canCalculateAutomatically(jar)) {
            BigDecimal currentAmount = currentAmount(jar.getId(), null);
            return new SavingsJarApplyYieldResponse(
                    jar.getId(),
                    jar.getName(),
                    null,
                    targetDate,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    currentAmount,
                    "O cofrinho não está configurado para rendimento automático por CDI."
            );
        }

        LocalDate from = nextCalculationDate(jar);
        if (from == null || from.isAfter(targetDate)) {
            BigDecimal currentAmount = currentAmount(jar.getId(), null);
            return new SavingsJarApplyYieldResponse(
                    jar.getId(),
                    jar.getName(),
                    from,
                    targetDate,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    currentAmount,
                    "Não existem rendimentos pendentes para este cofrinho."
            );
        }

        List<BcbSgsService.DailyRate> rates = bcbSgsService.getCdiRates(from, targetDate);
        BigDecimal totalYield = BigDecimal.ZERO;
        int created = 0;
        LocalDate lastProcessedRateDate = null;

        for (BcbSgsService.DailyRate dailyRate : rates) {
            LocalDate rateDate = dailyRate.referenceDate();
            if (jar.isBusinessDaysOnly() && isWeekend(rateDate)) {
                continue;
            }
            lastProcessedRateDate = rateDate;

            if (movementRepository.existsBySavingsJarIdAndTypeAndOccurredOn(jar.getId(), SavingsJarMovementType.YIELD, rateDate)) {
                continue;
            }

            BigDecimal baseAmount = currentAmount(jar.getId(), rateDate.minusDays(1));
            if (baseAmount.signum() <= 0) {
                continue;
            }

            BigDecimal appliedRate = toAppliedRate(dailyRate.value(), jar.getYieldPercentage());
            BigDecimal yieldAmount = baseAmount.multiply(appliedRate).setScale(2, RoundingMode.HALF_UP);
            if (yieldAmount.signum() <= 0) {
                continue;
            }

            SavingsJarMovement movement = new SavingsJarMovement();
            movement.setSavingsJar(jar);
            movement.setType(SavingsJarMovementType.YIELD);
            movement.setAmount(yieldAmount);
            movement.setOccurredOn(rateDate);
            movement.setDescription("Rendimento automático por CDI");
            movement.setSource(TransactionSource.IMPORTED);
            movement.setBaseAmount(baseAmount.setScale(2, RoundingMode.HALF_UP));
            movement.setRateApplied(appliedRate.setScale(6, RoundingMode.HALF_UP));
            movement.setRateReference("CDI SGS 12: " + dailyRate.value() + "%");
            movementRepository.save(movement);

            totalYield = totalYield.add(yieldAmount);
            created++;
        }

        if (lastProcessedRateDate != null) {
            jar.setLastYieldCalculationDate(lastProcessedRateDate);
            savingsJarRepository.save(jar);
        }

        return new SavingsJarApplyYieldResponse(
                jar.getId(),
                jar.getName(),
                from,
                targetDate,
                created,
                totalYield.setScale(2, RoundingMode.HALF_UP),
                currentAmount(jar.getId(), null),
                created == 0 ? "Nenhum rendimento novo foi aplicado." : "Rendimentos aplicados com sucesso."
        );
    }

    @Transactional
    public SavingsJarYieldPreviewResponse previewToday(SavingsJar jar, LocalDate today) {
        LocalDate reference = today == null ? LocalDate.now() : today;
        if (!canCalculateAutomatically(jar)) {
            return new SavingsJarYieldPreviewResponse(
                    reference,
                    currentAmount(jar.getId(), null),
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    false,
                    "Cofrinho sem cálculo automático de rendimento."
            );
        }
        if (jar.isBusinessDaysOnly() && isWeekend(reference)) {
            return new SavingsJarYieldPreviewResponse(
                    reference,
                    currentAmount(jar.getId(), null),
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    false,
                    "Rendimento previsto zerado porque a configuração considera apenas dias úteis."
            );
        }

        BigDecimal baseAmount = currentAmount(jar.getId(), null);
        if (baseAmount.signum() <= 0) {
            return new SavingsJarYieldPreviewResponse(
                    reference,
                    baseAmount,
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    false,
                    "Cofrinho sem saldo para cálculo de rendimento."
            );
        }

        BcbSgsService.DailyRate rate = bcbSgsService.getLatestCdiRateUntil(reference);
        BigDecimal appliedRate = toAppliedRate(rate.value(), jar.getYieldPercentage());
        BigDecimal projectedYield = baseAmount.multiply(appliedRate).setScale(2, RoundingMode.HALF_UP);
        boolean estimated = !rate.referenceDate().equals(reference);

        return new SavingsJarYieldPreviewResponse(
                rate.referenceDate(),
                baseAmount.setScale(2, RoundingMode.HALF_UP),
                rate.value(),
                appliedRate.setScale(6, RoundingMode.HALF_UP),
                projectedYield,
                estimated,
                estimated
                        ? "Prévia calculada com a última taxa CDI disponível até a data de referência."
                        : "Prévia calculada com a taxa CDI da data de referência."
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal currentAmount(Long savingsJarId, LocalDate until) {
        return movementRepository.calculateCurrentAmountUntil(savingsJarId, until).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalYield(Long savingsJarId, LocalDate until) {
        return movementRepository.calculateTotalYieldUntil(savingsJarId, until).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean canCalculateAutomatically(SavingsJar jar) {
        return jar.isYieldEnabled()
                && jar.getYieldCalculationType() == SavingsJarYieldCalculationType.CDI_PERCENTAGE
                && jar.getYieldPercentage() != null
                && jar.getYieldPercentage().signum() > 0;
    }

    private LocalDate nextCalculationDate(SavingsJar jar) {
        if (jar.getLastYieldCalculationDate() != null) {
            return jar.getLastYieldCalculationDate().plusDays(1);
        }
        if (jar.getYieldStartDate() != null) {
            return jar.getYieldStartDate();
        }
        return null;
    }

    private BigDecimal toAppliedRate(BigDecimal cdiDailyRatePercent, BigDecimal percentageOfCdi) {
        return cdiDailyRatePercent
                .divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP)
                .multiply(percentageOfCdi.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
