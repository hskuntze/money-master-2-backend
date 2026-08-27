package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemConfirmRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemConfirmationCandidateResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemConfirmationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;

@Service
public class CreditCardInvoiceItemService {

    private final CreditCardInvoiceItemRepository itemRepository;
    private final CreditCardInvoiceService invoiceService;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;
    private final FinancialTransactionService transactionService;

    public CreditCardInvoiceItemService(
            CreditCardInvoiceItemRepository itemRepository,
            CreditCardInvoiceService invoiceService,
            CurrentUserService currentUserService,
            CategoryService categoryService,
            FinancialTransactionService transactionService
    ) {
        this.itemRepository = itemRepository;
        this.invoiceService = invoiceService;
        this.currentUserService = currentUserService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<CreditCardInvoiceItemResponse> list(String ownerEmail, Long invoiceId) {
        invoiceService.findOwnedInvoice(ownerEmail, invoiceId);
        return itemRepository.findByInvoiceIdAndOwnerEmail(ownerEmail, invoiceId).stream()
                .map(CreditCardInvoiceItemResponse::from)
                .toList();
    }

    @Transactional
    public CreditCardInvoiceItemResponse create(String ownerEmail, Long invoiceId, CreditCardInvoiceItemCreateRequest request) {
        CreditCardInvoice invoice = invoiceService.findOwnedInvoice(ownerEmail, invoiceId);
        BigDecimal amount = normalizePositive(request.amount());
        LocalDate purchaseDate = requiredDate(request.purchaseDate(), "A data da compra e obrigatoria.");
        CreditCardInvoiceItemSourceType sourceType = normalizeSourceType(request.sourceType());
        FinancialTransaction transaction = resolveLinkedTransaction(
                ownerEmail,
                request.transactionId(),
                null,
                sourceType,
                amount,
                purchaseDate
        );
        CreditCardInvoiceItem item = new CreditCardInvoiceItem();
        item.setOwner(currentUserService.findUserByEmail(ownerEmail));
        item.setInvoice(invoice);
        item.setCategory(resolveCategory(ownerEmail, request.categoryId(), transaction));
        item.setDescription(required(request.description()));
        item.setAmount(amount);
        item.setPurchaseDate(purchaseDate);
        item.setCompetenceDate(request.competenceDate() == null ? purchaseDate : request.competenceDate());
        item.setSourceType(sourceType);
        item.setConfirmationStatus(resolveConfirmationStatus(request.confirmationStatus(), transaction));
        item.setSourceId(request.sourceId());
        item.setInstallmentNumber(request.installmentNumber());
        item.setTransaction(transaction);
        item.setNotes(optional(request.notes()));
        CreditCardInvoiceItem saved = itemRepository.save(item);
        transactionService.markAsCreditCardInvoiceItem(ownerEmail, transaction);
        invoiceService.syncInvoiceTotals(ownerEmail, invoice);
        return CreditCardInvoiceItemResponse.from(saved);
    }

    @Transactional
    public CreditCardInvoiceItemResponse update(String ownerEmail, Long itemId, CreditCardInvoiceItemUpdateRequest request) {
        CreditCardInvoiceItem item = findOwnedItem(ownerEmail, itemId);
        if (request.categoryId() != null) {
            item.setCategory(resolveCategory(ownerEmail, request.categoryId(), item.getTransaction()));
        }
        if (request.description() != null) {
            item.setDescription(required(request.description()));
        }
        if (request.amount() != null) {
            item.setAmount(normalizePositive(request.amount()));
        }
        if (request.purchaseDate() != null) {
            item.setPurchaseDate(request.purchaseDate());
        }
        if (request.competenceDate() != null) {
            item.setCompetenceDate(request.competenceDate());
        }
        if (request.sourceType() != null) {
            item.setSourceType(normalizeSourceType(request.sourceType()));
        }
        if (request.confirmationStatus() != null) {
            item.setConfirmationStatus(resolveConfirmationStatus(request.confirmationStatus(), item.getTransaction()));
        }
        if (request.sourceId() != null) {
            item.setSourceId(request.sourceId());
        }
        if (request.installmentNumber() != null) {
            item.setInstallmentNumber(request.installmentNumber());
        }
        if (request.transactionId() != null) {
            FinancialTransaction transaction = resolveLinkedTransaction(
                    ownerEmail,
                    request.transactionId(),
                    item.getId(),
                    item.getSourceType(),
                    item.getAmount(),
                    item.getPurchaseDate()
            );
            item.setTransaction(transaction);
            item.setConfirmationStatus(CreditCardInvoiceItemConfirmationStatus.CONFIRMED);
            if (item.getCategory() == null && transaction.getCategory() != null) {
                item.setCategory(transaction.getCategory());
            }
            transactionService.markAsCreditCardInvoiceItem(ownerEmail, transaction);
        }
        if (request.notes() != null) {
            item.setNotes(optional(request.notes()));
        }
        invoiceService.syncInvoiceTotals(ownerEmail, item.getInvoice());
        return CreditCardInvoiceItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<CreditCardInvoiceItemConfirmationCandidateResponse> confirmationCandidates(String ownerEmail, Long itemId) {
        CreditCardInvoiceItem item = findOwnedItem(ownerEmail, itemId);
        CreditCardInvoice invoice = item.getInvoice();
        LocalDate from = invoice.getOpeningDate() == null
                ? invoice.getClosingDate().minusMonths(1).plusDays(1)
                : invoice.getOpeningDate();
        LocalDate to = invoice.getClosingDate();
        Long cardAccountId = invoice.getCreditCard().getAccount() == null
                ? null
                : invoice.getCreditCard().getAccount().getId();

        return transactionService.search(ownerEmail, from, to, cardAccountId, null, TransactionType.EXPENSE, null, null)
                .stream()
                .filter(transaction -> transaction.creditCardInvoiceItemId() == null)
                .filter(transaction -> cardAccountId != null || transaction.account().type() == AccountType.CREDIT_CARD)
                .map(transaction -> candidate(item, transaction))
                .sorted(Comparator
                        .comparingInt(CreditCardInvoiceItemConfirmationCandidateResponse::score)
                        .reversed()
                        .thenComparing(candidate -> candidate.transaction().occurredOn(), Comparator.reverseOrder())
                        .thenComparing(candidate -> candidate.transaction().id(), Comparator.reverseOrder()))
                .limit(10)
                .toList();
    }

    @Transactional
    public CreditCardInvoiceItemResponse confirm(String ownerEmail, Long itemId, CreditCardInvoiceItemConfirmRequest request) {
        CreditCardInvoiceItem item = findOwnedItem(ownerEmail, itemId);
        CreditCardInvoiceItemConfirmRequest safeRequest = request == null
                ? new CreditCardInvoiceItemConfirmRequest(null, null, null, null, null)
                : request;

        if (safeRequest.amount() != null) {
            item.setAmount(normalizePositive(safeRequest.amount()));
        }
        if (safeRequest.purchaseDate() != null) {
            item.setPurchaseDate(safeRequest.purchaseDate());
            if (item.getCompetenceDate() == null) {
                item.setCompetenceDate(safeRequest.purchaseDate());
            }
        }
        if (safeRequest.categoryId() != null) {
            item.setCategory(resolveCategory(ownerEmail, safeRequest.categoryId(), item.getTransaction()));
        }
        if (safeRequest.transactionId() != null) {
            FinancialTransaction transaction = resolveLinkedTransaction(
                    ownerEmail,
                    safeRequest.transactionId(),
                    item.getId(),
                    item.getSourceType(),
                    item.getAmount(),
                    item.getPurchaseDate()
            );
            item.setTransaction(transaction);
            if (item.getCategory() == null && transaction.getCategory() != null) {
                item.setCategory(transaction.getCategory());
            }
            transactionService.markAsCreditCardInvoiceItem(ownerEmail, transaction);
        }
        if (safeRequest.notes() != null) {
            item.setNotes(optional(safeRequest.notes()));
        }

        item.setConfirmationStatus(CreditCardInvoiceItemConfirmationStatus.CONFIRMED);
        invoiceService.syncInvoiceTotals(ownerEmail, item.getInvoice());
        return CreditCardInvoiceItemResponse.from(item);
    }

    @Transactional
    public void delete(String ownerEmail, Long itemId) {
        CreditCardInvoiceItem item = findOwnedItem(ownerEmail, itemId);
        CreditCardInvoice invoice = item.getInvoice();
        itemRepository.delete(item);
        itemRepository.flush();
        invoiceService.syncInvoiceTotals(ownerEmail, invoice);
    }

    private CreditCardInvoiceItemConfirmationCandidateResponse candidate(
            CreditCardInvoiceItem item,
            FinancialTransactionResponse transaction
    ) {
        boolean amountMatches = item.getAmount() != null
                && transaction.amount() != null
                && item.getAmount().compareTo(transaction.amount().setScale(2, RoundingMode.HALF_UP)) == 0;
        boolean dateMatches = item.getPurchaseDate() != null
                && item.getPurchaseDate().equals(transaction.occurredOn());
        boolean categoryMatches = item.getCategory() != null
                && transaction.category() != null
                && item.getCategory().getId().equals(transaction.category().id());
        boolean descriptionMatches = descriptionMatches(item.getDescription(), transaction.description());
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (amountMatches) {
            score += 50;
            reasons.add("Mesmo valor");
        }
        if (dateMatches) {
            score += 30;
            reasons.add("Mesma data");
        }
        if (categoryMatches) {
            score += 10;
            reasons.add("Mesma categoria");
        }
        if (descriptionMatches) {
            score += 10;
            reasons.add("Descricao parecida");
        }

        return new CreditCardInvoiceItemConfirmationCandidateResponse(
                transaction,
                score,
                amountMatches,
                dateMatches,
                categoryMatches,
                reasons
        );
    }

    private boolean descriptionMatches(String itemDescription, String transactionDescription) {
        if (itemDescription == null || transactionDescription == null) {
            return false;
        }
        String itemText = itemDescription.toLowerCase(Locale.ROOT).trim();
        String transactionText = transactionDescription.toLowerCase(Locale.ROOT).trim();
        return itemText.length() >= 3
                && transactionText.length() >= 3
                && (itemText.contains(transactionText) || transactionText.contains(itemText));
    }

    @Transactional(readOnly = true)
    public CreditCardInvoiceItem findOwnedItem(String ownerEmail, Long itemId) {
        return itemRepository.findByIdAndOwnerEmail(itemId, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Item de fatura nao encontrado."));
    }

    private Category resolveCategory(String ownerEmail, Long categoryId, FinancialTransaction transaction) {
        if (categoryId == null) {
            return transaction == null ? null : transaction.getCategory();
        }
        Category category = categoryService.findAvailableCategory(ownerEmail, categoryId);
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("A categoria do item da fatura precisa ser de despesa.");
        }
        return category;
    }

    private FinancialTransaction resolveLinkedTransaction(
            String ownerEmail,
            Long transactionId,
            Long ignoredItemId,
            CreditCardInvoiceItemSourceType sourceType,
            BigDecimal amount,
            LocalDate purchaseDate
    ) {
        if (transactionId == null) {
            return null;
        }
        if (itemRepository.existsByTransactionIdExcludingItem(transactionId, ignoredItemId)) {
            throw new BusinessException("Esta transacao ja esta vinculada a outro item de fatura.");
        }
        FinancialTransaction transaction = transactionService.findOwnedTransaction(ownerEmail, transactionId);
        if (transaction.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("Somente despesas podem ser vinculadas como compra no cartao.");
        }
        if (sourceType == CreditCardInvoiceItemSourceType.CARD_PURCHASE
                && amount != null
                && transaction.getAmount() != null
                && amount.compareTo(transaction.getAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessException("O valor do item da fatura deve ser igual ao valor da transacao vinculada.");
        }
        if (sourceType == CreditCardInvoiceItemSourceType.CARD_PURCHASE
                && purchaseDate != null
                && transaction.getOccurredOn() != null
                && !purchaseDate.equals(transaction.getOccurredOn())) {
            throw new BusinessException("A data da compra deve ser igual a data da transacao vinculada.");
        }
        return transaction;
    }

    private CreditCardInvoiceItemSourceType normalizeSourceType(CreditCardInvoiceItemSourceType sourceType) {
        return sourceType == null ? CreditCardInvoiceItemSourceType.CARD_PURCHASE : sourceType;
    }

    private CreditCardInvoiceItemConfirmationStatus resolveConfirmationStatus(
            CreditCardInvoiceItemConfirmationStatus requestedStatus,
            FinancialTransaction transaction
    ) {
        if (transaction != null) {
            return CreditCardInvoiceItemConfirmationStatus.CONFIRMED;
        }
        return requestedStatus == null ? CreditCardInvoiceItemConfirmationStatus.CONFIRMED : requestedStatus;
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("A descricao e obrigatoria.");
        }
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDate requiredDate(LocalDate value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        return value;
    }

    private BigDecimal normalizePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("O valor deve ser maior que zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
