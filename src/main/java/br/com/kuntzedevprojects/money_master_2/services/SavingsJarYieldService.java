package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.config.properties.SavingsJarYieldProperties;
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
    private final SavingsJarYieldProperties yieldProperties;
    private final BrazilianBusinessDayService businessDayService;

    public SavingsJarYieldService(
            SavingsJarRepository savingsJarRepository,
            SavingsJarMovementRepository movementRepository,
            BcbSgsService bcbSgsService,
            SavingsJarYieldProperties yieldProperties,
            BrazilianBusinessDayService businessDayService
    ) {
        this.savingsJarRepository = savingsJarRepository;
        this.movementRepository = movementRepository;
        this.bcbSgsService = bcbSgsService;
        this.yieldProperties = yieldProperties;
        this.businessDayService = businessDayService;
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
        LocalDate targetDate = resolveTargetDate(jar, to);

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
            refreshLastYieldCalculationDate(jar);
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
        Map<LocalDate, BcbSgsService.DailyRate> ratesByDate = new HashMap<>();
        rates.forEach(rate -> ratesByDate.put(rate.referenceDate(), rate));

        BigDecimal totalYield = BigDecimal.ZERO;
        int created = 0;
        boolean foundApplicableRate = false;
        boolean usedLatestAvailableRate = false;
        boolean skippedByExistingMovement = false;
        boolean skippedByMissingBaseAmount = false;

        for (LocalDate calculationDate = from; !calculationDate.isAfter(targetDate); calculationDate = calculationDate.plusDays(1)) {
            if (!businessDayService.isEligibleYieldDate(calculationDate, jar.isBusinessDaysOnly(), jar.isUseBrazilianHolidays())) {
                continue;
            }

            RateForCalculation rateForCalculation = resolveRateForCalculationDate(calculationDate, targetDate, ratesByDate);
            if (rateForCalculation == null) {
                continue;
            }

            foundApplicableRate = true;
            usedLatestAvailableRate = usedLatestAvailableRate || rateForCalculation.estimated();

            String referenceKey = cdiReferenceKey(calculationDate);
            if (movementRepository.existsBySavingsJarIdAndReferenceKey(jar.getId(), referenceKey)
                    || movementRepository.existsBySavingsJarIdAndTypeAndOccurredOn(jar.getId(), SavingsJarMovementType.YIELD, calculationDate)) {
                skippedByExistingMovement = true;
                continue;
            }

            BigDecimal baseAmount = currentAmount(jar.getId(), calculationDate.minusDays(1));
            if (baseAmount.signum() <= 0) {
                skippedByMissingBaseAmount = true;
                continue;
            }

            BcbSgsService.DailyRate dailyRate = rateForCalculation.rate();
            BigDecimal appliedRate = toAppliedRate(dailyRate.value(), jar.getYieldPercentage());
            BigDecimal yieldAmount = baseAmount.multiply(appliedRate).setScale(2, RoundingMode.HALF_UP);
            if (yieldAmount.signum() <= 0) {
                skippedByMissingBaseAmount = true;
                continue;
            }

            SavingsJarMovement movement = new SavingsJarMovement();
            movement.setSavingsJar(jar);
            movement.setType(SavingsJarMovementType.YIELD);
            movement.setAmount(yieldAmount);
            movement.setOccurredOn(calculationDate);
            movement.setDescription(rateForCalculation.estimated()
                    ? "Rendimento automático por CDI com última taxa disponível"
                    : "Rendimento automático por CDI");
            movement.setSource(TransactionSource.IMPORTED);
            movement.setBaseAmount(baseAmount.setScale(2, RoundingMode.HALF_UP));
            movement.setRateApplied(appliedRate.setScale(6, RoundingMode.HALF_UP));
            movement.setRateReference(rateReference(dailyRate, calculationDate, rateForCalculation.estimated()));
            movement.setReferenceKey(referenceKey);
            movementRepository.save(movement);
            totalYield = totalYield.add(yieldAmount);
            created++;
        }

        refreshLastYieldCalculationDate(jar);

        String message = buildApplyMessage(created, foundApplicableRate, usedLatestAvailableRate, skippedByExistingMovement, skippedByMissingBaseAmount);

        return new SavingsJarApplyYieldResponse(
                jar.getId(),
                jar.getName(),
                from,
                targetDate,
                created,
                totalYield.setScale(2, RoundingMode.HALF_UP),
                currentAmount(jar.getId(), null),
                message
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
        if (!businessDayService.isEligibleYieldDate(reference, jar.isBusinessDaysOnly(), jar.isUseBrazilianHolidays())) {
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

    private LocalDate resolveTargetDate(SavingsJar jar, LocalDate to) {
        if (to != null) {
            return to;
        }
        LocalDate today = LocalDate.now(ZoneId.of(yieldProperties.getZoneId()));
        return businessDayService.previousEligibleDate(today, jar.isBusinessDaysOnly(), jar.isUseBrazilianHolidays());
    }

    private LocalDate nextCalculationDate(SavingsJar jar) {
        LocalDate start = jar.getYieldStartDate();
        LocalDate latestAppliedByHistory = latestAppliedYieldDate(jar);

        if (latestAppliedByHistory == null) {
            return start;
        }

        LocalDate next = latestAppliedByHistory.plusDays(1);
        if (start != null && next.isBefore(start)) {
            return start;
        }
        return next;
    }

    private LocalDate latestAppliedYieldDate(SavingsJar jar) {
        return movementRepository.findLatestMovementDateBySavingsJarIdAndTypes(
                jar.getId(),
                Set.of(SavingsJarMovementType.YIELD, SavingsJarMovementType.INITIAL_YIELD, SavingsJarMovementType.YIELD_ADJUSTMENT)
        ).orElse(null);
    }

    private void refreshLastYieldCalculationDate(SavingsJar jar) {
        LocalDate latestAppliedByHistory = latestAppliedYieldDate(jar);
        if (latestAppliedByHistory != null && !latestAppliedByHistory.equals(jar.getLastYieldCalculationDate())) {
            jar.setLastYieldCalculationDate(latestAppliedByHistory);
            savingsJarRepository.save(jar);
        }
    }

    private RateForCalculation resolveRateForCalculationDate(
            LocalDate calculationDate,
            LocalDate targetDate,
            Map<LocalDate, BcbSgsService.DailyRate> ratesByDate
    ) {
        BcbSgsService.DailyRate exactRate = ratesByDate.get(calculationDate);
        if (exactRate != null) {
            return new RateForCalculation(exactRate, false);
        }

        // O SGS do Banco Central nem sempre disponibiliza a taxa do próprio dia no momento do clique.
        // Para manter a operação diária do usuário, somente a data-alvo recebe fallback para a última taxa CDI publicada.
        if (!calculationDate.equals(targetDate)) {
            return null;
        }

        try {
            BcbSgsService.DailyRate latestRate = bcbSgsService.getLatestCdiRateUntil(calculationDate);
            if (latestRate == null || latestRate.referenceDate().isAfter(calculationDate)) {
                return null;
            }
            return new RateForCalculation(latestRate, !latestRate.referenceDate().equals(calculationDate));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String rateReference(BcbSgsService.DailyRate dailyRate, LocalDate calculationDate, boolean estimated) {
        String reference = "CDI:" + dailyRate.value() + "%;BC:" + dailyRate.referenceDate();
        if (estimated) {
            reference += ";APLICADO:" + calculationDate;
        }
        return limit(reference, 80);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildApplyMessage(
            int created,
            boolean foundApplicableRate,
            boolean usedLatestAvailableRate,
            boolean skippedByExistingMovement,
            boolean skippedByMissingBaseAmount
    ) {
        if (created > 0 && usedLatestAvailableRate) {
            return "Rendimentos aplicados com sucesso usando a última taxa CDI disponível no Banco Central para a data sem taxa própria publicada.";
        }
        if (created > 0) {
            return "Rendimentos aplicados com sucesso.";
        }
        if (!foundApplicableRate) {
            return "Nenhum rendimento novo foi aplicado porque ainda não há taxa CDI disponível no Banco Central para o período pendente.";
        }
        if (skippedByExistingMovement) {
            return "Nenhum rendimento novo foi aplicado porque as datas disponíveis já foram processadas.";
        }
        if (skippedByMissingBaseAmount) {
            return "Nenhum rendimento novo foi aplicado porque não havia saldo-base no dia anterior às datas pendentes.";
        }
        return "Nenhum rendimento novo foi aplicado.";
    }

    private record RateForCalculation(BcbSgsService.DailyRate rate, boolean estimated) {
    }

    private String cdiReferenceKey(LocalDate rateDate) {
        return "CDI_YIELD:" + rateDate;
    }

    private BigDecimal toAppliedRate(BigDecimal cdiDailyRatePercent, BigDecimal percentageOfCdi) {
        return cdiDailyRatePercent
                .divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP)
                .multiply(percentageOfCdi.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP));
    }

}
