package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.config.properties.SavingsJarYieldProperties;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.ToolSavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarApplyYieldResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarMovementRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarMovementResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarYieldPreviewResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;
import br.com.kuntzedevprojects.money_master_2.entities.SavingsJarMovement;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarYieldCalculationType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarMovementRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.SavingsJarRepository;

@Service
public class SavingsJarService {

    private final SavingsJarRepository savingsJarRepository;
    private final SavingsJarMovementRepository movementRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final SavingsJarYieldService yieldService;
    private final SavingsJarYieldProperties yieldProperties;

    public SavingsJarService(
            SavingsJarRepository savingsJarRepository,
            SavingsJarMovementRepository movementRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            SavingsJarYieldService yieldService,
            SavingsJarYieldProperties yieldProperties
    ) {
        this.savingsJarRepository = savingsJarRepository;
        this.movementRepository = movementRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.yieldService = yieldService;
        this.yieldProperties = yieldProperties;
    }

    @Transactional
    public List<SavingsJarResponse> list(String ownerEmail) {
        return savingsJarRepository.findByOwnerEmailWithAccount(ownerEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavingsJarResponse get(String ownerEmail, Long id) {
        return toResponse(findOwnedJar(ownerEmail, id));
    }

    @Transactional
    public SavingsJarSummaryResponse summary(String ownerEmail) {
        List<SavingsJarResponse> jars = list(ownerEmail);
        BigDecimal totalSaved = jars.stream().map(SavingsJarResponse::currentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTarget = jars.stream().map(SavingsJarResponse::targetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalYield = jars.stream().map(SavingsJarResponse::totalYield).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalTarget.subtract(totalSaved).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageProgress = BigDecimal.ZERO;
        if (!jars.isEmpty()) {
            averageProgress = jars.stream()
                    .map(SavingsJarResponse::progressPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(jars.size()), 2, RoundingMode.HALF_UP);
        }

        return new SavingsJarSummaryResponse(
                totalSaved.setScale(2, RoundingMode.HALF_UP),
                totalTarget.setScale(2, RoundingMode.HALF_UP),
                totalYield.setScale(2, RoundingMode.HALF_UP),
                remaining,
                averageProgress,
                jars.size(),
                jars
        );
    }

    @Transactional
    public SavingsJarResponse create(String ownerEmail, SavingsJarCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        String name = normalizeRequired(request.name(), "O nome do cofrinho é obrigatório.");
        String institutionName = normalizeNullable(request.institutionName());

        ensureNoDuplicate(ownerEmail, name, institutionName, null);

        BigDecimal currentAmount = nullToZero(request.currentAmount());
        BigDecimal currentYield = nullToZero(request.currentYieldAmount());
        validateCurrentAmounts(currentAmount, currentYield);

        LocalDate today = today();
        LocalDate initialMovementDate = request.lastYieldCalculationDate() != null
                ? request.lastYieldCalculationDate()
                : today;

        SavingsJar jar = new SavingsJar();
        jar.setOwner(owner);
        jar.setName(name);
        jar.setInstitutionName(institutionName);
        jar.setDescription(normalizeNullable(request.description()));
        jar.setTargetAmount(nullToZero(request.targetAmount()));
        jar.setTargetDate(request.targetDate());
        jar.setImageUrl(normalizeNullable(request.imageUrl()));
        jar.setIcon(normalizeNullableOrDefault(request.icon(), "piggy-bank"));
        jar.setColor(normalizeNullableOrDefault(request.color(), "#2563eb"));
        jar.setActive(request.active() == null || request.active());
        jar.setYieldEnabled(request.yieldEnabled() != null && request.yieldEnabled());
        jar.setYieldCalculationType(request.yieldCalculationType() == null ? SavingsJarYieldCalculationType.MANUAL : request.yieldCalculationType());
        jar.setYieldPercentage(nullToZero(request.yieldPercentage()));
        jar.setBusinessDaysOnly(request.businessDaysOnly() == null || request.businessDaysOnly());
        jar.setUseBrazilianHolidays(request.useBrazilianHolidays() != null && request.useBrazilianHolidays());
        jar.setYieldStartDate(request.yieldStartDate() == null ? today : request.yieldStartDate());
        jar.setLastYieldCalculationDate(resolveInitialLastYieldDate(request, currentAmount, currentYield, today));
        if (request.linkedAccountId() != null) {
            jar.setLinkedAccount(accountService.findOwnedAccount(ownerEmail, request.linkedAccountId()));
        }
        validateYieldConfiguration(jar);

        SavingsJar saved = savingsJarRepository.save(jar);
        BigDecimal principalAmount = currentAmount.subtract(currentYield).setScale(2, RoundingMode.HALF_UP);
        if (principalAmount.signum() > 0) {
            saveMovement(saved, SavingsJarMovementType.INITIAL_BALANCE, principalAmount, initialMovementDate,
                    "Saldo inicial importado do cofrinho", TransactionSource.MANUAL, null, null, null, null);
        }
        if (currentYield.signum() > 0) {
            saveMovement(saved, SavingsJarMovementType.INITIAL_YIELD, currentYield, initialMovementDate,
                    "Rendimento já acumulado informado na criação", TransactionSource.MANUAL, null, null, null, null);
        }

        return toResponse(saved);
    }

    @Transactional
    public SavingsJarResponse update(String ownerEmail, Long id, SavingsJarUpdateRequest request) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            String institution = request.institutionName() == null ? jar.getInstitutionName() : normalizeNullable(request.institutionName());
            ensureNoDuplicate(ownerEmail, name, institution, id);
            jar.setName(name);
        }
        if (request.institutionName() != null) {
            String institution = normalizeNullable(request.institutionName());
            ensureNoDuplicate(ownerEmail, jar.getName(), institution, id);
            jar.setInstitutionName(institution);
        }
        if (request.description() != null) {
            jar.setDescription(normalizeNullable(request.description()));
        }
        if (request.targetAmount() != null) {
            jar.setTargetAmount(nullToZero(request.targetAmount()));
        }
        if (request.targetDate() != null) {
            jar.setTargetDate(request.targetDate());
        }
        if (request.imageUrl() != null) {
            jar.setImageUrl(normalizeNullable(request.imageUrl()));
        }
        if (request.icon() != null) {
            jar.setIcon(normalizeNullable(request.icon()));
        }
        if (request.color() != null) {
            jar.setColor(normalizeNullable(request.color()));
        }
        if (Boolean.TRUE.equals(request.removeLinkedAccount())) {
            jar.setLinkedAccount(null);
        } else if (request.linkedAccountId() != null) {
            Account account = accountService.findOwnedAccount(ownerEmail, request.linkedAccountId());
            jar.setLinkedAccount(account);
        }
        if (request.active() != null) {
            jar.setActive(request.active());
        }
        if (request.yieldEnabled() != null) {
            jar.setYieldEnabled(request.yieldEnabled());
        }
        if (request.yieldCalculationType() != null) {
            jar.setYieldCalculationType(request.yieldCalculationType());
        }
        if (request.yieldPercentage() != null) {
            jar.setYieldPercentage(nullToZero(request.yieldPercentage()));
        }
        if (request.businessDaysOnly() != null) {
            jar.setBusinessDaysOnly(request.businessDaysOnly());
        }
        if (request.useBrazilianHolidays() != null) {
            jar.setUseBrazilianHolidays(request.useBrazilianHolidays());
        }
        if (request.yieldStartDate() != null) {
            jar.setYieldStartDate(request.yieldStartDate());
        }
        if (request.lastYieldCalculationDate() != null) {
            jar.setLastYieldCalculationDate(request.lastYieldCalculationDate());
        }

        validateYieldConfiguration(jar);
        return toResponse(jar);
    }

    @Transactional
    public void deactivate(String ownerEmail, Long id) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);
        jar.setActive(false);
    }

    @Transactional
    public SavingsJarMovementResponse deposit(String ownerEmail, Long id, SavingsJarMovementRequest request) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);
        SavingsJarMovement movement = saveMovement(jar, SavingsJarMovementType.DEPOSIT, normalizeAmount(request.amount()),
                request.occurredOn() == null ? today() : request.occurredOn(),
                normalizeNullableOrDefault(request.description(), "Aporte no cofrinho"),
                request.source() == null ? TransactionSource.MANUAL : request.source(), null, null, null,
                normalizeNullable(request.notes()));
        return SavingsJarMovementResponse.from(movement);
    }

    @Transactional
    public SavingsJarMovementResponse withdraw(String ownerEmail, Long id, SavingsJarMovementRequest request) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);
        BigDecimal amount = normalizeAmount(request.amount());
        BigDecimal currentAmount = yieldService.currentAmount(jar.getId(), null);
        if (amount.compareTo(currentAmount) > 0) {
            throw new BusinessException("O valor de retirada não pode ser maior que o saldo atual do cofrinho.");
        }
        SavingsJarMovement movement = saveMovement(jar, SavingsJarMovementType.WITHDRAWAL, amount,
                request.occurredOn() == null ? today() : request.occurredOn(),
                normalizeNullableOrDefault(request.description(), "Retirada do cofrinho"),
                request.source() == null ? TransactionSource.MANUAL : request.source(), null, null, null,
                normalizeNullable(request.notes()));
        return SavingsJarMovementResponse.from(movement);
    }

    @Transactional
    public SavingsJarMovementResponse registerManualYield(String ownerEmail, Long id, SavingsJarMovementRequest request) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);
        LocalDate occurredOn = request.occurredOn() == null ? today() : request.occurredOn();
        SavingsJarMovement movement = saveMovement(jar, SavingsJarMovementType.YIELD, normalizeAmount(request.amount()),
                occurredOn,
                normalizeNullableOrDefault(request.description(), "Rendimento informado manualmente"),
                request.source() == null ? TransactionSource.MANUAL : request.source(),
                yieldService.currentAmount(jar.getId(), occurredOn.minusDays(1)),
                null,
                "Rendimento manual informado pelo usuário",
                normalizeNullable(request.notes()));
        if (jar.getLastYieldCalculationDate() == null || jar.getLastYieldCalculationDate().isBefore(occurredOn)) {
            jar.setLastYieldCalculationDate(occurredOn);
        }
        return SavingsJarMovementResponse.from(movement);
    }

    @Transactional(readOnly = true)
    public List<SavingsJarMovementResponse> movements(String ownerEmail, Long id) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);
        return movementRepository.findBySavingsJarIdOrderByOccurredOnDescIdDesc(jar.getId())
                .stream()
                .map(SavingsJarMovementResponse::from)
                .toList();
    }

    @Transactional
    public SavingsJarApplyYieldResponse applyPendingYield(String ownerEmail, Long id, LocalDate to) {
        SavingsJar jar = findOwnedJar(ownerEmail, id);
        return yieldService.applyPendingYields(jar, to == null ? today() : to);
    }

    @Transactional
    public List<SavingsJarApplyYieldResponse> applyPendingYieldsForUser(String ownerEmail, LocalDate to) {
        LocalDate target = to == null ? today() : to;
        return savingsJarRepository.findByOwnerEmailWithAccount(ownerEmail)
                .stream()
                .filter(SavingsJar::isActive)
                .filter(SavingsJar::isYieldEnabled)
                .map(jar -> yieldService.applyPendingYields(jar, target))
                .toList();
    }

    @Transactional
    public ToolSavingsJarResponse createFromAi(
            String ownerEmail,
            String name,
            String institutionName,
            BigDecimal targetAmount,
            String targetDateText,
            String imageUrl,
            BigDecimal currentAmount,
            BigDecimal currentYieldAmount,
            BigDecimal cdiPercentage,
            String originalMessage
    ) {
        SavingsJarCreateRequest request = new SavingsJarCreateRequest(
                name,
                institutionName,
                null,
                targetAmount,
                parseDateOrNull(targetDateText),
                imageUrl,
                null,
                null,
                null,
                currentAmount,
                currentYieldAmount,
                true,
                cdiPercentage != null && cdiPercentage.signum() > 0,
                cdiPercentage != null && cdiPercentage.signum() > 0 ? SavingsJarYieldCalculationType.CDI_PERCENTAGE : SavingsJarYieldCalculationType.MANUAL,
                cdiPercentage,
                true,
                false,
                today(),
                currentAmount != null && currentAmount.signum() > 0 ? today() : null
        );
        SavingsJarResponse response = create(ownerEmail, request);
        return toToolResponse(response, "Cofrinho criado com sucesso.");
    }

    @Transactional
    public ToolSavingsJarResponse depositFromAi(String ownerEmail, String name, String institutionName, BigDecimal amount, String occurredOnText, String originalMessage) {
        SavingsJar jar = resolveForAi(ownerEmail, name, institutionName);
        SavingsJarMovementRequest request = new SavingsJarMovementRequest(
                amount,
                parseDateOrToday(occurredOnText),
                "Aporte informado pelo chat",
                TransactionSource.AI_CHAT,
                originalMessage
        );
        deposit(ownerEmail, jar.getId(), request);
        return toToolResponse(toResponse(jar), "Aporte registrado com sucesso.");
    }

    @Transactional
    public ToolSavingsJarResponse withdrawFromAi(String ownerEmail, String name, String institutionName, BigDecimal amount, String occurredOnText, String originalMessage) {
        SavingsJar jar = resolveForAi(ownerEmail, name, institutionName);
        SavingsJarMovementRequest request = new SavingsJarMovementRequest(
                amount,
                parseDateOrToday(occurredOnText),
                "Retirada informada pelo chat",
                TransactionSource.AI_CHAT,
                originalMessage
        );
        withdraw(ownerEmail, jar.getId(), request);
        return toToolResponse(toResponse(jar), "Retirada registrada com sucesso.");
    }

    @Transactional
    public ToolSavingsJarResponse registerYieldFromAi(String ownerEmail, String name, String institutionName, BigDecimal amount, String occurredOnText, String originalMessage) {
        SavingsJar jar = resolveForAi(ownerEmail, name, institutionName);
        SavingsJarMovementRequest request = new SavingsJarMovementRequest(
                amount,
                parseDateOrToday(occurredOnText),
                "Rendimento informado pelo chat",
                TransactionSource.AI_CHAT,
                originalMessage
        );
        registerManualYield(ownerEmail, jar.getId(), request);
        return toToolResponse(toResponse(jar), "Rendimento registrado com sucesso.");
    }

    @Transactional(readOnly = true)
    public SavingsJar findOwnedJar(String ownerEmail, Long id) {
        return savingsJarRepository.findByIdAndOwnerEmailWithAccount(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cofrinho não encontrado."));
    }

    @Transactional(readOnly = true)
    public SavingsJar resolveForAi(String ownerEmail, String name, String institutionName) {
        String normalizedName = normalizeRequired(name, "Informe o nome do cofrinho.");
        return savingsJarRepository.findByOwnerEmailAndNameAndInstitution(ownerEmail, normalizedName, normalizeNullable(institutionName))
                .orElseThrow(() -> new ResourceNotFoundException("Cofrinho não encontrado para este usuário."));
    }

    private SavingsJarResponse toResponse(SavingsJar jar) {
        BigDecimal currentAmount = yieldService.currentAmount(jar.getId(), null);
        BigDecimal totalYield = yieldService.totalYield(jar.getId(), null);
        SavingsJarYieldPreviewResponse projectedYieldToday;
        try {
            projectedYieldToday = yieldService.previewToday(jar, today());
        } catch (Exception ex) {
            projectedYieldToday = new SavingsJarYieldPreviewResponse(
                    today(), currentAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    true, "Não foi possível calcular a prévia de rendimento: " + ex.getMessage()
            );
        }
        return SavingsJarResponse.from(jar, currentAmount, totalYield, projectedYieldToday);
    }

    private ToolSavingsJarResponse toToolResponse(SavingsJarResponse response, String message) {
        BigDecimal projected = response.projectedYieldToday() == null ? BigDecimal.ZERO : response.projectedYieldToday().projectedYieldAmount();
        LocalDate reference = response.projectedYieldToday() == null ? null : response.projectedYieldToday().referenceDate();
        return new ToolSavingsJarResponse(
                response.id(),
                response.name(),
                response.institutionName(),
                response.currentAmount(),
                response.targetAmount(),
                response.progressPercentage(),
                response.totalYield(),
                projected,
                reference,
                message
        );
    }

    private SavingsJarMovement saveMovement(
            SavingsJar jar,
            SavingsJarMovementType type,
            BigDecimal amount,
            LocalDate occurredOn,
            String description,
            TransactionSource source,
            BigDecimal baseAmount,
            BigDecimal rateApplied,
            String rateReference,
            String notes
    ) {
        SavingsJarMovement movement = new SavingsJarMovement();
        movement.setSavingsJar(jar);
        movement.setType(type);
        movement.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        movement.setOccurredOn(occurredOn == null ? today() : occurredOn);
        movement.setDescription(normalizeRequired(description, "A descrição da movimentação é obrigatória."));
        movement.setSource(source == null ? TransactionSource.MANUAL : source);
        movement.setBaseAmount(baseAmount == null ? null : baseAmount.setScale(2, RoundingMode.HALF_UP));
        movement.setRateApplied(rateApplied == null ? null : rateApplied.setScale(6, RoundingMode.HALF_UP));
        movement.setRateReference(normalizeNullable(rateReference));
        if (source == TransactionSource.AI_CHAT) {
            movement.setAiRawMessage(normalizeNullable(notes));
        } else {
            movement.setNotes(notes);
        }
        return movementRepository.save(movement);
    }

    private void ensureNoDuplicate(String ownerEmail, String name, String institutionName, Long ignoreId) {
        savingsJarRepository.findByOwnerEmailAndNameAndInstitution(ownerEmail, name, institutionName)
                .filter(existing -> ignoreId == null || !existing.getId().equals(ignoreId))
                .ifPresent(existing -> {
                    throw new BusinessException("Já existe um cofrinho com este nome para esta instituição.");
                });
    }

    private void validateCurrentAmounts(BigDecimal currentAmount, BigDecimal currentYield) {
        if (currentAmount.signum() < 0 || currentYield.signum() < 0) {
            throw new BusinessException("Valores do cofrinho não podem ser negativos.");
        }
        if (currentYield.compareTo(currentAmount) > 0) {
            throw new BusinessException("O rendimento acumulado não pode ser maior que o valor atual do cofrinho.");
        }
    }

    private void validateYieldConfiguration(SavingsJar jar) {
        if (jar.isYieldEnabled()
                && jar.getYieldCalculationType() == SavingsJarYieldCalculationType.CDI_PERCENTAGE
                && (jar.getYieldPercentage() == null || jar.getYieldPercentage().signum() <= 0)) {
            throw new BusinessException("Informe o percentual do CDI para cofrinhos com rendimento automático por CDI.");
        }
    }

    private LocalDate resolveInitialLastYieldDate(SavingsJarCreateRequest request, BigDecimal currentAmount, BigDecimal currentYield, LocalDate today) {
        if (request.lastYieldCalculationDate() != null) {
            return request.lastYieldCalculationDate();
        }
        if (currentAmount.signum() > 0 || currentYield.signum() > 0) {
            return today;
        }
        return null;
    }

    private LocalDate parseDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return today();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BusinessException("A data deve estar no formato ISO yyyy-MM-dd.");
        }
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BusinessException("A data deve estar no formato ISO yyyy-MM-dd.");
        }
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(yieldProperties.getZoneId()));
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("O valor deve ser maior que zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeNullableOrDefault(String value, String defaultValue) {
        String normalized = normalizeNullable(value);
        return normalized == null ? defaultValue : normalized;
    }
}
