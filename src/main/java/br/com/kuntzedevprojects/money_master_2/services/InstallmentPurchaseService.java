package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentEntryPaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPaymentResultResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentSource;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentMode;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemSettlementOrigin;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseEntryRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;

import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoiceService;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardService;

@Service
public class InstallmentPurchaseService {

    private final InstallmentPurchaseRepository purchaseRepository;
    private final InstallmentPurchaseEntryRepository entryRepository;
    private final MonthlyPlanItemRepository planItemRepository;
    private final CreditCardInvoiceItemRepository invoiceItemRepository;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final FinancialPeriodService financialPeriodService;
    private final CreditCardService creditCardService;
    private final CreditCardInvoiceService creditCardInvoiceService;

    public InstallmentPurchaseService(
            InstallmentPurchaseRepository purchaseRepository,
            InstallmentPurchaseEntryRepository entryRepository,
            MonthlyPlanItemRepository planItemRepository,
            CreditCardInvoiceItemRepository invoiceItemRepository,
            CurrentUserService currentUserService,
            AccountService accountService,
            CategoryService categoryService,
            FinancialPeriodService financialPeriodService,
            CreditCardService creditCardService,
            CreditCardInvoiceService creditCardInvoiceService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.entryRepository = entryRepository;
        this.planItemRepository = planItemRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.financialPeriodService = financialPeriodService;
        this.creditCardService = creditCardService;
        this.creditCardInvoiceService = creditCardInvoiceService;
    }

    @Transactional
    public List<InstallmentPurchaseResponse> list(String ownerEmail) {
        return purchaseRepository.findByOwnerEmailWithEntries(ownerEmail)
                .stream()
                .map(InstallmentPurchaseResponse::from)
                .toList();
    }

    @Transactional
    public InstallmentPurchaseResponse get(String ownerEmail, Long id) {
        return InstallmentPurchaseResponse.from(findOwnedPurchase(ownerEmail, id));
    }

    @Transactional(readOnly = true)
    public List<br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseEntryResponse> listInstallments(String ownerEmail, Long id) {
        return findOwnedPurchase(ownerEmail, id).getEntries()
                .stream()
                .sorted(Comparator.comparing(InstallmentPurchaseEntry::getInstallmentNumber))
                .map(br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseEntryResponse::from)
                .toList();
    }

    @Transactional
    public InstallmentPurchaseResponse create(String ownerEmail, InstallmentPurchaseCreateRequest request) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        int count = normalizeCount(request.installmentCount());
        BigDecimal installmentAmount = resolveInstallmentAmount(request.totalAmount(), request.installmentAmount(), count);
        BigDecimal totalAmount = resolveTotalAmount(request.totalAmount(), installmentAmount, count);
        List<BigDecimal> installmentAmounts = resolveInstallmentAmounts(request.totalAmount(), installmentAmount, totalAmount, count);
        LocalDate firstDueDate = request.firstDueDate();
        LocalDate purchaseDate = request.purchaseDate() == null ? LocalDate.now() : request.purchaseDate();
        if (firstDueDate == null) {
            throw new BusinessException("A data da primeira parcela é obrigatória.");
        }
        InstallmentPaymentMode paymentMode = request.paymentMode() == null ? InstallmentPaymentMode.DIRECT_PAYABLE : request.paymentMode();
        CreditCard creditCard = paymentMode == InstallmentPaymentMode.CREDIT_CARD ? resolveCreditCard(ownerEmail, request.creditCardId()) : null;
        Account account = creditCard != null && creditCard.getAccount() != null ? creditCard.getAccount() : accountService.getOrCreateDefaultAccount(ownerEmail);
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
        purchase.setPaymentMode(paymentMode);
        purchase.setCreditCard(creditCard);
        purchase.setNotes(normalizeNullable(request.notes()));
        InstallmentPurchase saved = purchaseRepository.save(purchase);

        for (int index = 0; index < count; index++) {
            LocalDate dueDate = firstDueDate.plusMonths(index);
            BigDecimal currentInstallmentAmount = installmentAmounts.get(index);
            FinancialPeriod period = financialPeriodService.findOrCreateForDate(ownerEmail, dueDate);
            InstallmentPurchaseEntry entry = new InstallmentPurchaseEntry();
            entry.setOwner(owner);
            entry.setPurchase(saved);
            entry.setFinancialPeriod(period);
            entry.setInstallmentNumber(index + 1);
            entry.setDueDate(dueDate);
            entry.setAmount(currentInstallmentAmount);
            entry.setPaymentSource(InstallmentPaymentSource.NONE);
            entry.setNotes("Parcela " + (index + 1) + " de " + count + " da compra parcelada.");
            if (paymentMode == InstallmentPaymentMode.CREDIT_CARD) {
                CreditCardInvoice invoice = creditCardInvoiceService.findOrCreateForInstallment(ownerEmail, creditCard.getId(), dueDate);
                CreditCardInvoiceItem invoiceItem = createInvoiceItem(owner, saved, invoice, purchaseDate, dueDate, index + 1, currentInstallmentAmount, category);
                entry.setFinancialPeriod(invoice.getCycle());
                entry.setInvoiceItem(invoiceItem);
                entry.setStatus(InstallmentEntryStatus.IN_INVOICE);
                if (index == 0) {
                    saved.setFirstInvoice(invoice);
                }
                creditCardInvoiceService.syncInvoiceTotals(ownerEmail, invoice);
            } else {
                MonthlyPlanItem item = createMonthlyPlanItem(owner, saved, period, dueDate, index + 1, currentInstallmentAmount, category, account);
                entry.setMonthlyPlanItem(item);
                entry.setStatus(InstallmentEntryStatus.POSTED);
            }
            saved.addEntry(entry);
        }
        refreshPurchaseStatus(saved);
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
                InstallmentPaymentMode.DIRECT_PAYABLE,
                null,
                notes
        );
        return create(ownerEmail, request);
    }

    @Transactional
    public InstallmentPurchaseResponse markEntryPaid(String ownerEmail, Long entryId, InstallmentEntryPaymentRequest request) {
        User actor = currentUserService.findUserByEmail(ownerEmail);
        InstallmentPurchaseEntry entry = findOwnedEntry(ownerEmail, entryId);
        if (entry.getStatus() == InstallmentEntryStatus.CANCELED || entry.getPurchase().getStatus() == InstallmentPurchaseStatus.CANCELED) {
            throw new BusinessException("Não é possível dar baixa em uma parcela cancelada.");
        }
        ensureEntryNotLinkedToInvoice(entry);
        LocalDate paidOn = request == null || request.paidOn() == null ? LocalDate.now() : request.paidOn();
        markEntryPaid(entry, paidOn, InstallmentPaymentSource.MANUAL, actor);
        refreshPurchaseStatus(entry.getPurchase());
        return InstallmentPurchaseResponse.from(findOwnedPurchase(ownerEmail, entry.getPurchase().getId()));
    }

    @Transactional
    public InstallmentPurchaseResponse reopenEntryPayment(String ownerEmail, Long entryId) {
        User actor = currentUserService.findUserByEmail(ownerEmail);
        InstallmentPurchaseEntry entry = findOwnedEntry(ownerEmail, entryId);
        if (entry.getStatus() == InstallmentEntryStatus.CANCELED || entry.getPurchase().getStatus() == InstallmentPurchaseStatus.CANCELED) {
            throw new BusinessException("Não é possível reabrir uma parcela cancelada.");
        }
        ensureEntryNotLinkedToInvoice(entry);
        reopenEntry(entry, actor);
        refreshPurchaseStatus(entry.getPurchase());
        return InstallmentPurchaseResponse.from(findOwnedPurchase(ownerEmail, entry.getPurchase().getId()));
    }

    @Transactional
    public InstallmentPaymentResultResponse markInstallmentsFromAi(
            String ownerEmail,
            Long purchaseId,
            String purchaseDescription,
            Integer installmentsToPay,
            Integer targetPaidInstallments,
            String paidOnText
    ) {
        User actor = currentUserService.findUserByEmail(ownerEmail);
        InstallmentPurchase purchase = resolvePurchaseForAi(ownerEmail, purchaseId, purchaseDescription);
        if (purchase.getStatus() == InstallmentPurchaseStatus.CANCELED) {
            throw new BusinessException("A compra parcelada \"" + purchase.getDescription() + "\" está cancelada.");
        }

        LocalDate paidOn = parseDateOrToday(paidOnText);
        int alreadyPaid = countPaidEntries(purchase);
        int targetPaid = targetPaidInstallments == null
                ? alreadyPaid + normalizeInstallmentsToPay(installmentsToPay)
                : targetPaidInstallments;

        if (targetPaid < 0) {
            throw new BusinessException("A quantidade de parcelas pagas não pode ser negativa.");
        }
        if (targetPaid > purchase.getInstallmentCount()) {
            throw new BusinessException("A compra \"" + purchase.getDescription() + "\" possui apenas " + purchase.getInstallmentCount() + " parcelas.");
        }
        if (targetPaid <= alreadyPaid) {
            String message = "A compra \"" + purchase.getDescription() + "\" já possui " + alreadyPaid + " de " + purchase.getInstallmentCount() + " parcelas pagas.";
            return new InstallmentPaymentResultResponse(message, InstallmentPurchaseResponse.from(purchase));
        }

        int toMark = targetPaid - alreadyPaid;
        List<InstallmentPurchaseEntry> pendingEntries = purchase.getEntries()
                .stream()
                .filter(entry -> entry.getStatus() != InstallmentEntryStatus.CANCELED)
                .filter(entry -> entry.getStatus() != InstallmentEntryStatus.PAID)
                .sorted(Comparator.comparing(InstallmentPurchaseEntry::getInstallmentNumber))
                .toList();

        if (pendingEntries.isEmpty()) {
            String message = "A compra \"" + purchase.getDescription() + "\" já está totalmente paga.";
            return new InstallmentPaymentResultResponse(message, InstallmentPurchaseResponse.from(purchase));
        }
        if (toMark > pendingEntries.size()) {
            throw new BusinessException("A compra \"" + purchase.getDescription() + "\" possui apenas " + pendingEntries.size() + " parcela(s) pendente(s).");
        }

        pendingEntries.stream()
                .limit(toMark)
                .map(this::linkedInvoiceDescription)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(invoiceDescription -> {
                    throw new BusinessException("Essa parcela está vinculada à fatura \"" + invoiceDescription + "\". Você deseja marcar a fatura inteira como paga ou apenas registrar essa parcela como conferida?");
                });

        pendingEntries.stream()
                .limit(toMark)
                .forEach(entry -> markEntryPaid(entry, paidOn, InstallmentPaymentSource.CHAT, actor));

        refreshPurchaseStatus(purchase);
        InstallmentPurchase refreshed = findOwnedPurchase(ownerEmail, purchase.getId());
        int paidAfter = countPaidEntries(refreshed);
        String message = "Perfeito. Marquei como paga(s) " + toMark + " parcela(s) da compra \"" + refreshed.getDescription()
                + "\". Agora ela possui " + paidAfter + " de " + refreshed.getInstallmentCount() + " parcelas pagas.";
        return new InstallmentPaymentResultResponse(message, InstallmentPurchaseResponse.from(refreshed));
    }

    @Transactional
    public void cancel(String ownerEmail, Long id) {
        InstallmentPurchase purchase = findOwnedPurchase(ownerEmail, id);
        purchase.setStatus(InstallmentPurchaseStatus.CANCELED);
        purchase.getEntries().forEach(entry -> {
            entry.setStatus(InstallmentEntryStatus.CANCELED);
            entry.setPaidOn(null);
            entry.setPaymentSource(InstallmentPaymentSource.NONE);
            entry.setPaidBy(null);
            entry.setPaymentRegisteredAt(null);
            MonthlyPlanItem item = entry.getMonthlyPlanItem();
            if (item != null && item.getStatus() != MonthlyPlanItemStatus.PAID) {
                item.setStatus(MonthlyPlanItemStatus.CANCELED);
            }
            CreditCardInvoiceItem invoiceItem = entry.getInvoiceItem();
            if (invoiceItem != null) {
                CreditCardInvoice invoice = invoiceItem.getInvoice();
                invoiceItemRepository.delete(invoiceItem);
                entry.setInvoiceItem(null);
                creditCardInvoiceService.syncInvoiceTotals(ownerEmail, invoice);
            }
        });
    }

    private void markEntryPaid(InstallmentPurchaseEntry entry, LocalDate paidOn, InstallmentPaymentSource source, User actor) {
        if (isEntryLinkedToInvoice(entry)) {
            return;
        }
        entry.setStatus(InstallmentEntryStatus.PAID);
        entry.setPaidOn(paidOn == null ? LocalDate.now() : paidOn);
        entry.setPaymentSource(source == null ? InstallmentPaymentSource.MANUAL : source);
        entry.setPaidBy(actor);
        entry.setPaymentRegisteredAt(Instant.now());
        MonthlyPlanItem item = entry.getMonthlyPlanItem();
        if (item != null && item.getStatus() != MonthlyPlanItemStatus.CANCELED) {
            BigDecimal amount = nullToZero(item.getExpectedAmount()).signum() > 0
                    ? nullToZero(item.getExpectedAmount())
                    : nullToZero(entry.getAmount());
            item.setActualAmount(amount);
            item.setPaidOn(entry.getPaidOn());
            item.setStatus(MonthlyPlanItemStatus.PAID);
        }
    }

    private void reopenEntry(InstallmentPurchaseEntry entry, User actor) {
        ensureEntryNotLinkedToInvoice(entry);
        MonthlyPlanItem item = entry.getMonthlyPlanItem();
        entry.setStatus(item == null ? InstallmentEntryStatus.PENDING : InstallmentEntryStatus.POSTED);
        entry.setPaidOn(null);
        entry.setPaymentSource(InstallmentPaymentSource.MANUAL);
        entry.setPaidBy(actor);
        entry.setPaymentRegisteredAt(Instant.now());
        if (item != null && item.getStatus() != MonthlyPlanItemStatus.CANCELED) {
            item.setActualAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setPaidOn(null);
            item.setStatus(MonthlyPlanItemStatus.PENDING);
        }
    }

    private void refreshPurchaseStatus(InstallmentPurchase purchase) {
        if (purchase == null || purchase.getStatus() == InstallmentPurchaseStatus.CANCELED) {
            return;
        }
        boolean allPaid = purchase.getEntries()
                .stream()
                .filter(entry -> entry.getStatus() != InstallmentEntryStatus.CANCELED)
                .allMatch(entry -> entry.getStatus() == InstallmentEntryStatus.PAID);
        purchase.setStatus(allPaid ? InstallmentPurchaseStatus.COMPLETED : InstallmentPurchaseStatus.ACTIVE);
    }

    private int countPaidEntries(InstallmentPurchase purchase) {
        return (int) purchase.getEntries()
                .stream()
                .filter(entry -> entry.getStatus() == InstallmentEntryStatus.PAID)
                .count();
    }

    private int normalizeInstallmentsToPay(Integer installmentsToPay) {
        if (installmentsToPay == null) {
            return 1;
        }
        if (installmentsToPay <= 0) {
            throw new BusinessException("Informe uma quantidade de parcelas maior que zero.");
        }
        return installmentsToPay;
    }

    private void ensureEntryNotLinkedToInvoice(InstallmentPurchaseEntry entry) {
        String invoiceDescription = linkedInvoiceDescription(entry);
        if (invoiceDescription != null) {
            throw new BusinessException("Esta parcela está vinculada à fatura \"" + invoiceDescription + "\". Marque a fatura inteira como paga ou desvincule a parcela antes de dar baixa individual.");
        }
    }

    private boolean isEntryLinkedToInvoice(InstallmentPurchaseEntry entry) {
        return linkedInvoiceDescription(entry) != null || entry.getInvoiceItem() != null;
    }

    private String linkedInvoiceDescription(InstallmentPurchaseEntry entry) {
        if (entry != null && entry.getInvoiceItem() != null && entry.getInvoiceItem().getInvoice() != null) {
            CreditCardInvoice invoice = entry.getInvoiceItem().getInvoice();
            return "Fatura " + invoice.getCreditCard().getName() + " - "
                    + String.format("%02d/%04d", invoice.getReferenceMonth(), invoice.getReferenceYear());
        }
        MonthlyPlanItem item = entry == null ? null : entry.getMonthlyPlanItem();
        if (item == null || item.getParentItem() == null) {
            return null;
        }
        if (item.getAggregationType() != MonthlyPlanItemAggregationType.GROUP_CHILD) {
            return null;
        }
        return item.getParentItem().getDescription();
    }

    private CreditCard resolveCreditCard(String ownerEmail, Long creditCardId) {
        if (creditCardId == null) {
            throw new BusinessException("Selecione o cartao de credito da compra parcelada.");
        }
        return creditCardService.findOwnedCard(ownerEmail, creditCardId);
    }

    private CreditCardInvoiceItem createInvoiceItem(
            User owner,
            InstallmentPurchase purchase,
            CreditCardInvoice invoice,
            LocalDate purchaseDate,
            LocalDate dueDate,
            int installmentNumber,
            BigDecimal installmentAmount,
            Category category
    ) {
        CreditCardInvoiceItem item = new CreditCardInvoiceItem();
        item.setOwner(owner);
        item.setInvoice(invoice);
        item.setCategory(category);
        item.setDescription(purchase.getDescription() + " (" + installmentNumber + "/" + purchase.getInstallmentCount() + ")");
        item.setAmount(installmentAmount);
        item.setPurchaseDate(purchaseDate);
        item.setCompetenceDate(dueDate);
        item.setSourceType(CreditCardInvoiceItemSourceType.INSTALLMENT);
        item.setSourceId(purchase.getId());
        item.setInstallmentNumber(installmentNumber);
        item.setNotes("Parcela gerada automaticamente pela compra parcelada #" + purchase.getId() + ".");
        return invoiceItemRepository.save(item);
    }

    private InstallmentPurchase findOwnedPurchase(String ownerEmail, Long id) {
        return purchaseRepository.findByIdAndOwnerEmailWithEntries(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Compra parcelada não encontrada."));
    }

    private InstallmentPurchaseEntry findOwnedEntry(String ownerEmail, Long id) {
        return entryRepository.findByIdAndOwnerEmailWithRelations(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Parcela da compra parcelada não encontrada."));
    }

    private InstallmentPurchase resolvePurchaseForAi(String ownerEmail, Long purchaseId, String description) {
        if (purchaseId != null) {
            return findOwnedPurchase(ownerEmail, purchaseId);
        }
        String normalizedQuery = normalizeComparable(normalizeRequired(description, "Informe qual compra parcelada deve receber baixa."));
        List<InstallmentPurchase> matches = purchaseRepository.findByOwnerEmailWithEntries(ownerEmail)
                .stream()
                .filter(purchase -> purchase.getStatus() != InstallmentPurchaseStatus.CANCELED)
                .filter(purchase -> {
                    String normalizedDescription = normalizeComparable(purchase.getDescription());
                    return normalizedDescription.contains(normalizedQuery) || normalizedQuery.contains(normalizedDescription);
                })
                .toList();
        if (matches.isEmpty()) {
            throw new BusinessException("Não encontrei compra parcelada parecida com \"" + description + "\".");
        }
        List<InstallmentPurchase> exactMatches = matches.stream()
                .filter(purchase -> normalizeComparable(purchase.getDescription()).equals(normalizedQuery))
                .toList();
        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }
        if (matches.size() > 1) {
            String options = matches.stream()
                    .limit(5)
                    .map(purchase -> purchase.getId() + ". " + purchase.getDescription() + " - " + purchase.getInstallmentCount() + " parcelas")
                    .collect(Collectors.joining("\n"));
            throw new BusinessException("Encontrei mais de uma compra parecida com \"" + description + "\". Qual delas você quer atualizar?\n" + options);
        }
        return matches.get(0);
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
        item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        item.setSettlementOrigin(MonthlyPlanItemSettlementOrigin.DIRECT);
        item.setPaidByParent(false);
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

    private List<BigDecimal> resolveInstallmentAmounts(BigDecimal requestedTotalAmount, BigDecimal installmentAmount, BigDecimal totalAmount, int count) {
        if (requestedTotalAmount == null || requestedTotalAmount.signum() <= 0) {
            return java.util.stream.IntStream.range(0, count)
                    .mapToObj(index -> installmentAmount.setScale(2, RoundingMode.HALF_UP))
                    .toList();
        }
        BigDecimal baseAmount = installmentAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal accumulated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        java.util.ArrayList<BigDecimal> amounts = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            BigDecimal amount = index == count - 1
                    ? totalAmount.subtract(accumulated).setScale(2, RoundingMode.HALF_UP)
                    : baseAmount;
            if (amount.signum() <= 0) {
                throw new BusinessException("O valor total e a quantidade de parcelas geram parcelas zeradas. Ajuste os valores da compra.");
            }
            amounts.add(amount);
            accumulated = accumulated.add(amount).setScale(2, RoundingMode.HALF_UP);
        }
        return amounts;
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

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeComparable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
