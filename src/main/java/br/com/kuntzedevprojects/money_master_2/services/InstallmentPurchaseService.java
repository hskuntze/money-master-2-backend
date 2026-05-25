package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;

@Service
public class InstallmentPurchaseService {

    private final InstallmentPurchaseRepository purchaseRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final FinancialPeriodService financialPeriodService;

    public InstallmentPurchaseService(
            InstallmentPurchaseRepository purchaseRepository,
            MonthlyPlanItemRepository planItemRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            FinancialPeriodService financialPeriodService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.planItemRepository = planItemRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.financialPeriodService = financialPeriodService;
    }

    @Transactional(readOnly = true)
    public List<InstallmentPurchaseResponse> list(String ownerEmail) {
        return purchaseRepository.findByOwnerEmailWithEntries(ownerEmail)
                .stream()
                .map(InstallmentPurchaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstallmentPurchaseResponse get(String ownerEmail, Long id) {
        return InstallmentPurchaseResponse.from(findOwnedPurchase(ownerEmail, id));
    }

    @Transactional
    public InstallmentPurchaseResponse create(String ownerEmail, InstallmentPurchaseCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        int count = normalizeCount(request.installmentCount());
        BigDecimal installmentAmount = resolveInstallmentAmount(request.totalAmount(), request.installmentAmount(), count);
        BigDecimal totalAmount = resolveTotalAmount(request.totalAmount(), installmentAmount, count);
        LocalDate firstDueDate = request.firstDueDate();
        LocalDate purchaseDate = request.purchaseDate() == null ? LocalDate.now() : request.purchaseDate();
        if (firstDueDate == null) {
            throw new BusinessException("A data da primeira parcela é obrigatória.");
        }
        Account account = accountService.getOrCreateDefaultAccount(ownerEmail);
        Category category = request.categoryId() == null ? null : categoryService.findAvailableCategory(ownerEmail, request.categoryId());
        if (category != null && category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("A categoria da compra parcelada precisa ser de despesa.");
        }

        InstallmentPurchase purchase = new InstallmentPurchase();
        purchase.setOwner(owner);
        purchase.setAccount(account);
        purchase.setCategory(category);
        purchase.setDescription(normalizeRequired(request.description(), "A descrição da compra é obrigatória."));
        purchase.setTotalAmount(totalAmount);
        purchase.setInstallmentCount(count);
        purchase.setInstallmentAmount(installmentAmount);
        purchase.setPurchaseDate(purchaseDate);
        purchase.setFirstDueDate(firstDueDate);
        purchase.setLastDueDate(firstDueDate.plusMonths(count - 1));
        purchase.setStatus(InstallmentPurchaseStatus.ACTIVE);
        purchase.setNotes(normalizeNullable(request.notes()));
        InstallmentPurchase saved = purchaseRepository.save(purchase);

        for (int index = 0; index < count; index++) {
            LocalDate dueDate = firstDueDate.plusMonths(index);
            FinancialPeriod period = financialPeriodService.findOrCreateForDate(ownerEmail, dueDate);
            MonthlyPlanItem item = createMonthlyPlanItem(owner, saved, period, dueDate, index + 1, installmentAmount, category, account);
            InstallmentPurchaseEntry entry = new InstallmentPurchaseEntry();
            entry.setOwner(owner);
            entry.setPurchase(saved);
            entry.setFinancialPeriod(period);
            entry.setMonthlyPlanItem(item);
            entry.setInstallmentNumber(index + 1);
            entry.setDueDate(dueDate);
            entry.setAmount(installmentAmount);
            entry.setStatus(item.getStatus() == MonthlyPlanItemStatus.PAID ? InstallmentEntryStatus.PAID : InstallmentEntryStatus.POSTED);
            entry.setNotes("Parcela " + (index + 1) + " de " + count + " da compra parcelada.");
            saved.addEntry(entry);
        }
        return InstallmentPurchaseResponse.from(saved);
    }

    @Transactional
    public InstallmentPurchaseResponse createFromAi(
            String ownerEmail,
            String description,
            BigDecimal totalAmount,
            BigDecimal installmentAmount,
            Integer installmentCount,
            String purchaseDateText,
            String firstDueDateText,
            String categoryName,
            String notes
    ) {
        Category category = categoryName == null || categoryName.isBlank()
                ? null
                : categoryService.resolveForAi(ownerEmail, null, categoryName, TransactionType.EXPENSE);
        InstallmentPurchaseCreateRequest request = new InstallmentPurchaseCreateRequest(
                description,
                totalAmount,
                installmentCount,
                installmentAmount,
                parseDateOrNull(purchaseDateText),
                parseDateOrToday(firstDueDateText),
                category == null ? null : category.getId(),
                notes
        );
        return create(ownerEmail, request);
    }

    @Transactional
    public void cancel(String ownerEmail, Long id) {
        InstallmentPurchase purchase = findOwnedPurchase(ownerEmail, id);
        purchase.setStatus(InstallmentPurchaseStatus.CANCELED);
        purchase.getEntries().forEach(entry -> {
            entry.setStatus(InstallmentEntryStatus.CANCELED);
            MonthlyPlanItem item = entry.getMonthlyPlanItem();
            if (item != null && item.getStatus() != MonthlyPlanItemStatus.PAID) {
                item.setStatus(MonthlyPlanItemStatus.CANCELED);
            }
        });
    }

    private InstallmentPurchase findOwnedPurchase(String ownerEmail, Long id) {
        return purchaseRepository.findByIdAndOwnerEmailWithEntries(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Compra parcelada não encontrada."));
    }

    private MonthlyPlanItem createMonthlyPlanItem(
            User owner,
            InstallmentPurchase purchase,
            FinancialPeriod period,
            LocalDate dueDate,
            int installmentNumber,
            BigDecimal installmentAmount,
            Category category,
            Account account
    ) {
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setOwner(owner);
        item.setFinancialPeriod(period);
        item.setAccount(account);
        item.setCategory(category);
        item.setType(TransactionType.EXPENSE);
        item.setDescription(purchase.getDescription() + " (" + installmentNumber + "/" + purchase.getInstallmentCount() + ")");
        item.setExpectedAmount(installmentAmount);
        item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setDueDate(clampDate(dueDate, period));
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(MonthlyPlanItemNature.FIXED);
        item.setRecurring(false);
        item.setNotes("Parcela gerada automaticamente pela compra parcelada #" + purchase.getId() + ".");
        return planItemRepository.save(item);
    }

    private LocalDate clampDate(LocalDate dueDate, FinancialPeriod period) {
        if (dueDate.isBefore(period.getStartDate())) {
            return period.getStartDate();
        }
        if (dueDate.isAfter(period.getEndDate())) {
            return period.getEndDate();
        }
        return dueDate;
    }

    private int normalizeCount(Integer count) {
        if (count == null || count < 1) {
            throw new BusinessException("A quantidade de parcelas deve ser maior que zero.");
        }
        if (count > 120) {
            throw new BusinessException("A quantidade de parcelas não pode ultrapassar 120.");
        }
        return count;
    }

    private BigDecimal resolveInstallmentAmount(BigDecimal totalAmount, BigDecimal installmentAmount, int count) {
        if (installmentAmount != null && installmentAmount.signum() > 0) {
            return installmentAmount.setScale(2, RoundingMode.HALF_UP);
        }
        if (totalAmount != null && totalAmount.signum() > 0) {
            return totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        throw new BusinessException("Informe o valor total ou o valor da parcela.");
    }

    private BigDecimal resolveTotalAmount(BigDecimal totalAmount, BigDecimal installmentAmount, int count) {
        if (totalAmount != null && totalAmount.signum() > 0) {
            return totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return installmentAmount.multiply(BigDecimal.valueOf(count)).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate parseDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value.trim());
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
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
}
