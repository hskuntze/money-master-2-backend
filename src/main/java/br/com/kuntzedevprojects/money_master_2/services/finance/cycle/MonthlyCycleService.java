package br.com.kuntzedevprojects.money_master_2.services.finance.cycle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodTurnoverRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleTurnoverRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialPeriodRepository;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

@Service
public class MonthlyCycleService {

    private static final DateTimeFormatter DEFAULT_NAME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinancialPeriodService financialPeriodService;
    private final FinancialPeriodRepository periodRepository;
    private final CurrentUserService currentUserService;

    public MonthlyCycleService(
            FinancialPeriodService financialPeriodService,
            FinancialPeriodRepository periodRepository,
            CurrentUserService currentUserService
    ) {
        this.financialPeriodService = financialPeriodService;
        this.periodRepository = periodRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public MonthlyCycleResponse create(String ownerEmail, MonthlyCycleCreateRequest request) {
        validateRange(request.startDate(), request.endDate());
        validateNoOverlap(ownerEmail, null, request.startDate(), request.endDate());

        FinancialPeriod period = new FinancialPeriod();
        period.setOwner(currentUserService.findUserByEmail(ownerEmail));
        period.setName(normalizeName(request.name(), request.startDate()));
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());
        period.setTurnoverDay(request.turnoverDay() == null ? request.startDate().getDayOfMonth() : request.turnoverDay());
        period.setStatus(resolveInitialStatus(request.startDate(), request.endDate()));

        return MonthlyCycleResponse.from(periodRepository.save(period));
    }

    @Transactional
    public MonthlyCycleResponse update(String ownerEmail, Long id, MonthlyCycleUpdateRequest request) {
        FinancialPeriodUpdateRequest legacyRequest = new FinancialPeriodUpdateRequest(
                request.name(),
                request.startDate(),
                request.endDate(),
                request.turnoverDay(),
                request.status()
        );
        return MonthlyCycleResponse.from(financialPeriodService.updatePeriod(ownerEmail, id, legacyRequest));
    }

    @Transactional
    public MonthlyCycleResponse turnover(String ownerEmail, MonthlyCycleTurnoverRequest request) {
        FinancialPeriodTurnoverRequest legacyRequest = new FinancialPeriodTurnoverRequest(
                request.turnoverDate(),
                request.newCycleName()
        );
        return MonthlyCycleResponse.from(financialPeriodService.turnover(ownerEmail, legacyRequest));
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Informe as datas inicial e final do ciclo mensal.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("A data final do ciclo mensal deve ser posterior ou igual a data inicial.");
        }
    }

    private void validateNoOverlap(String ownerEmail, Long periodId, LocalDate startDate, LocalDate endDate) {
        Long currentId = periodId == null ? -1L : periodId;
        if (!periodRepository.findOverlappingPeriods(ownerEmail, currentId, startDate, endDate).isEmpty()) {
            throw new BusinessException("Ja existe um ciclo mensal que se sobrepoe ao periodo informado.");
        }
    }

    private String normalizeName(String name, LocalDate startDate) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return "Ciclo " + DEFAULT_NAME_FORMATTER.format(startDate);
    }

    private FinancialPeriodStatus resolveInitialStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            return FinancialPeriodStatus.OPEN;
        }
        return FinancialPeriodStatus.SCHEDULED;
    }
}
