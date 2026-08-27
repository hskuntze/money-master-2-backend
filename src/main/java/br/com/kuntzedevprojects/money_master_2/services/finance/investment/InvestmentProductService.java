package br.com.kuntzedevprojects.money_master_2.services.finance.investment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentMovement;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.InvestmentMovementRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InvestmentProductRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;

@Service
public class InvestmentProductService {

    private final InvestmentProductRepository productRepository;
    private final InvestmentMovementRepository movementRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;

    public InvestmentProductService(
            InvestmentProductRepository productRepository,
            InvestmentMovementRepository movementRepository,
            CurrentUserService currentUserService,
            AccountService accountService
    ) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<InvestmentProductResponse> list(String ownerEmail) {
        return productRepository.findByOwnerEmailWithAccount(ownerEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvestmentProduct> listEntities(String ownerEmail) {
        return productRepository.findByOwnerEmailWithAccount(ownerEmail);
    }

    @Transactional(readOnly = true)
    public InvestmentProductResponse get(String ownerEmail, Long id) {
        return toResponse(findOwnedProduct(ownerEmail, id));
    }

    @Transactional
    public InvestmentProductResponse create(String ownerEmail, InvestmentProductCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        String name = normalizeRequired(request.name(), "O nome do produto financeiro é obrigatório.");
        if (productRepository.existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(ownerEmail, name)) {
            throw new BusinessException("Já existe um produto financeiro com este nome.");
        }

        InvestmentProduct product = new InvestmentProduct();
        product.setOwner(owner);
        product.setName(name);
        product.setTypeName(normalizeRequired(request.typeName(), "O tipo do produto financeiro é obrigatório."));
        product.setInstitutionName(normalizeNullable(request.institutionName()));
        product.setLiquidity(normalizeNullable(request.liquidity()));
        product.setActive(request.active() == null || request.active());
        product.setNotes(normalizeNullable(request.notes()));
        if (request.linkedAccountId() != null) {
            product.setLinkedAccount(accountService.findOwnedAccount(ownerEmail, request.linkedAccountId()));
        }

        InvestmentProduct saved = productRepository.save(product);
        LocalDate initialDate = request.initialBalanceDate() == null ? LocalDate.now() : request.initialBalanceDate();
        BigDecimal currentAmount = nullToZero(request.currentAmount());
        BigDecimal currentYieldAmount = nullToZero(request.currentYieldAmount());
        validateInitialAmounts(currentAmount, currentYieldAmount);

        BigDecimal principalAmount = currentAmount.subtract(currentYieldAmount).setScale(2, RoundingMode.HALF_UP);
        if (principalAmount.signum() > 0) {
            saveMovement(saved, InvestmentMovementType.INITIAL_BALANCE, principalAmount, initialDate,
                    "Saldo inicial informado", TransactionSource.MANUAL, null);
        }
        if (currentYieldAmount.signum() > 0) {
            saveMovement(saved, InvestmentMovementType.YIELD, currentYieldAmount, initialDate,
                    "Rendimento acumulado informado na criação", TransactionSource.MANUAL, null);
        }

        return toResponse(saved);
    }

    @Transactional
    public InvestmentProductResponse update(String ownerEmail, Long id, InvestmentProductUpdateRequest request) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, id);
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            productRepository.findByOwnerEmailIgnoreCaseAndNameIgnoreCase(ownerEmail, name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BusinessException("Já existe outro produto financeiro com este nome.");
                    });
            product.setName(name);
        }
        if (request.typeName() != null && !request.typeName().isBlank()) {
            product.setTypeName(request.typeName().trim());
        }
        if (request.institutionName() != null) {
            product.setInstitutionName(normalizeNullable(request.institutionName()));
        }
        if (Boolean.TRUE.equals(request.removeLinkedAccount())) {
            product.setLinkedAccount(null);
        } else if (request.linkedAccountId() != null) {
            Account account = accountService.findOwnedAccount(ownerEmail, request.linkedAccountId());
            product.setLinkedAccount(account);
        }
        if (request.liquidity() != null) {
            product.setLiquidity(normalizeNullable(request.liquidity()));
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        if (request.notes() != null) {
            product.setNotes(normalizeNullable(request.notes()));
        }
        if (request.currentAmount() != null) {
            correctCurrentAmount(product, request.currentAmount());
        }
        return toResponse(product);
    }

    @Transactional
    public boolean delete(String ownerEmail, Long id) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, id);
        if (movementRepository.existsByInvestmentProductId(product.getId())) {
            product.setActive(false);
            return false;
        }
        productRepository.delete(product);
        return true;
    }

    @Transactional(readOnly = true)
    public List<InvestmentMovementResponse> movements(String ownerEmail, Long productId) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, productId);
        return movementRepository.findByInvestmentProductIdOrderByOccurredOnDescIdDesc(product.getId())
                .stream()
                .map(InvestmentMovementResponse::from)
                .toList();
    }

    @Transactional
    public InvestmentMovementResponse contribute(String ownerEmail, Long productId, InvestmentMovementRequest request) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, productId);
        ensureActive(product);
        return InvestmentMovementResponse.from(saveMovement(
                product,
                InvestmentMovementType.CONTRIBUTION,
                normalizeAmount(request.amount()),
                request.occurredOn() == null ? LocalDate.now() : request.occurredOn(),
                normalizeNullableOrDefault(request.description(), "Aporte no produto financeiro"),
                request.source() == null ? TransactionSource.MANUAL : request.source(),
                normalizeNullable(request.notes())
        ));
    }

    @Transactional
    public InvestmentMovementResponse withdraw(String ownerEmail, Long productId, InvestmentMovementRequest request) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, productId);
        ensureActive(product);
        BigDecimal amount = normalizeAmount(request.amount());
        BigDecimal currentAmount = currentAmount(product.getId());
        if (amount.compareTo(currentAmount) > 0) {
            throw new BusinessException("O valor de resgate não pode ser maior que o saldo atual do produto financeiro.");
        }
        return InvestmentMovementResponse.from(saveMovement(
                product,
                InvestmentMovementType.WITHDRAWAL,
                amount,
                request.occurredOn() == null ? LocalDate.now() : request.occurredOn(),
                normalizeNullableOrDefault(request.description(), "Resgate do produto financeiro"),
                request.source() == null ? TransactionSource.MANUAL : request.source(),
                normalizeNullable(request.notes())
        ));
    }

    @Transactional
    public InvestmentMovementResponse registerYield(String ownerEmail, Long productId, InvestmentMovementRequest request) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, productId);
        ensureActive(product);
        return InvestmentMovementResponse.from(saveMovement(
                product,
                InvestmentMovementType.YIELD,
                normalizeAmount(request.amount()),
                request.occurredOn() == null ? LocalDate.now() : request.occurredOn(),
                normalizeNullableOrDefault(request.description(), "Rendimento informado"),
                request.source() == null ? TransactionSource.MANUAL : request.source(),
                normalizeNullable(request.notes())
        ));
    }

    @Transactional
    public InvestmentProductResponse reconcileCurrentAmount(String ownerEmail, Long productId, BigDecimal realCurrentAmount, LocalDate occurredOn, String notes) {
        InvestmentProduct product = findOwnedProduct(ownerEmail, productId);
        ensureActive(product);
        if (realCurrentAmount == null) {
            throw new BusinessException("Informe o saldo real atual do produto financeiro.");
        }
        correctCurrentAmount(product, realCurrentAmount, occurredOn == null ? LocalDate.now() : occurredOn, notes);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public InvestmentProduct findOwnedProduct(String ownerEmail, Long id) {
        return productRepository.findByIdAndOwnerEmailWithAccount(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Produto financeiro não encontrado."));
    }

    @Transactional(readOnly = true)
    public InvestmentProduct resolveForAi(String ownerEmail, String name, String institutionName) {
        String normalizedName = normalizeRequired(name, "Informe o nome do produto financeiro.");
        String normalizedInstitution = normalizeNullable(institutionName);

        List<InvestmentProduct> candidates = productRepository.findByOwnerEmailWithAccount(ownerEmail)
                .stream()
                .filter(InvestmentProduct::isActive)
                .filter(product -> normalizedInstitution == null
                        || Objects.equals(normalizeComparable(product.getInstitutionName()), normalizeComparable(normalizedInstitution)))
                .filter(product -> namesMatch(product.getName(), normalizedName))
                .sorted(Comparator.comparing(InvestmentProduct::getName))
                .toList();

        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (candidates.size() > 1) {
            throw new BusinessException("Encontrei mais de um produto financeiro com nome parecido. Informe também a instituição ou use o id do produto.");
        }
        throw new ResourceNotFoundException("Produto financeiro não encontrado para este usuário. Confira o nome ou peça para listar seus investimentos.");
    }

    private InvestmentProductResponse toResponse(InvestmentProduct product) {
        Long productId = product.getId();
        return InvestmentProductResponse.from(
                product,
                currentAmount(productId),
                nullToZero(movementRepository.calculateTotalContributed(productId)),
                nullToZero(movementRepository.calculateTotalWithdrawn(productId)),
                nullToZero(movementRepository.calculateTotalYield(productId))
        );
    }

    private void correctCurrentAmount(InvestmentProduct product, BigDecimal realCurrentAmount) {
        correctCurrentAmount(product, realCurrentAmount, LocalDate.now(), "Saldo real informado: " + nullToZero(realCurrentAmount).setScale(2, RoundingMode.HALF_UP));
    }

    private void correctCurrentAmount(InvestmentProduct product, BigDecimal realCurrentAmount, LocalDate occurredOn, String notes) {
        BigDecimal normalizedRealAmount = nullToZero(realCurrentAmount).setScale(2, RoundingMode.HALF_UP);
        if (normalizedRealAmount.signum() < 0) {
            throw new BusinessException("O saldo atual do produto financeiro não pode ser negativo.");
        }
        BigDecimal currentAmount = currentAmount(product.getId());
        BigDecimal adjustment = normalizedRealAmount.subtract(currentAmount).setScale(2, RoundingMode.HALF_UP);
        if (adjustment.signum() != 0) {
            saveMovement(product, InvestmentMovementType.ADJUSTMENT, adjustment, occurredOn == null ? LocalDate.now() : occurredOn,
                    adjustment.signum() > 0 ? "Ajuste positivo de saldo" : "Ajuste negativo de saldo",
                    TransactionSource.MANUAL,
                    normalizeNullableOrDefault(notes, "Saldo real informado: " + normalizedRealAmount));
        }
    }

    private InvestmentMovement saveMovement(
            InvestmentProduct product,
            InvestmentMovementType type,
            BigDecimal amount,
            LocalDate occurredOn,
            String description,
            TransactionSource source,
            String notes
    ) {
        InvestmentMovement movement = new InvestmentMovement();
        movement.setInvestmentProduct(product);
        movement.setType(type);
        movement.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        movement.setOccurredOn(occurredOn == null ? LocalDate.now() : occurredOn);
        movement.setDescription(normalizeRequired(description, "A descrição da movimentação é obrigatória."));
        movement.setSource(source == null ? TransactionSource.MANUAL : source);
        movement.setNotes(notes);
        return movementRepository.save(movement);
    }

    private void validateInitialAmounts(BigDecimal currentAmount, BigDecimal currentYieldAmount) {
        if (currentAmount.signum() < 0 || currentYieldAmount.signum() < 0) {
            throw new BusinessException("Valores do produto financeiro não podem ser negativos.");
        }
        if (currentYieldAmount.compareTo(currentAmount) > 0) {
            throw new BusinessException("O rendimento acumulado não pode ser maior que o saldo atual.");
        }
    }

    private void ensureActive(InvestmentProduct product) {
        if (!product.isActive()) {
            throw new BusinessException("Este produto financeiro está arquivado. Reative-o antes de registrar novas movimentações.");
        }
    }

    private BigDecimal currentAmount(Long productId) {
        return nullToZero(movementRepository.calculateCurrentAmountUntil(productId, null)).setScale(2, RoundingMode.HALF_UP);
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

    private boolean namesMatch(String candidate, String requested) {
        String left = normalizeComparable(candidate);
        String right = normalizeComparable(requested);
        if (left.equals(right) || left.contains(right) || right.contains(left)) {
            return true;
        }

        Set<String> leftTokens = Arrays.stream(left.split(" ")).collect(Collectors.toSet());
        Set<String> rightTokens = Arrays.stream(right.split(" ")).collect(Collectors.toSet());
        long common = rightTokens.stream()
                .filter(token -> !token.isBlank())
                .filter(leftTokens::contains)
                .count();
        int relevantTokens = (int) rightTokens.stream().filter(token -> !token.isBlank()).count();
        return relevantTokens > 0 && common >= Math.max(1, Math.ceil(relevantTokens * 0.6));
    }

    private String normalizeComparable(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replace("r$", " ")
                .replace("reais", " ")
                .replace("capitalizacao", "capitalizacao")
                .replaceAll("(\\d+)\\s*k\\b", "$1 mil")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
