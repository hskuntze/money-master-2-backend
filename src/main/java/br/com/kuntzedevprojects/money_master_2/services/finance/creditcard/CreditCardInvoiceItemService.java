package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;

@Service
public class CreditCardInvoiceItemService {

    private final CreditCardInvoiceItemRepository itemRepository;
    private final CreditCardInvoiceService invoiceService;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;

    public CreditCardInvoiceItemService(
            CreditCardInvoiceItemRepository itemRepository,
            CreditCardInvoiceService invoiceService,
            CurrentUserService currentUserService,
            CategoryService categoryService
    ) {
        this.itemRepository = itemRepository;
        this.invoiceService = invoiceService;
        this.currentUserService = currentUserService;
        this.categoryService = categoryService;
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
        CreditCardInvoiceItem item = new CreditCardInvoiceItem();
        item.setOwner(currentUserService.findUserByEmail(ownerEmail));
        item.setInvoice(invoice);
        item.setCategory(resolveCategory(ownerEmail, request.categoryId()));
        item.setDescription(required(request.description()));
        item.setAmount(normalizePositive(request.amount()));
        item.setPurchaseDate(requiredDate(request.purchaseDate(), "A data da compra e obrigatoria."));
        item.setCompetenceDate(request.competenceDate() == null ? request.purchaseDate() : request.competenceDate());
        item.setSourceType(request.sourceType() == null ? CreditCardInvoiceItemSourceType.MANUAL : request.sourceType());
        item.setSourceId(request.sourceId());
        item.setInstallmentNumber(request.installmentNumber());
        item.setTransactionId(request.transactionId());
        item.setNotes(optional(request.notes()));
        CreditCardInvoiceItem saved = itemRepository.save(item);
        invoiceService.syncInvoiceTotals(ownerEmail, invoice);
        return CreditCardInvoiceItemResponse.from(saved);
    }

    @Transactional
    public CreditCardInvoiceItemResponse update(String ownerEmail, Long itemId, CreditCardInvoiceItemUpdateRequest request) {
        CreditCardInvoiceItem item = findOwnedItem(ownerEmail, itemId);
        if (request.categoryId() != null) {
            item.setCategory(resolveCategory(ownerEmail, request.categoryId()));
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
            item.setSourceType(request.sourceType());
        }
        if (request.sourceId() != null) {
            item.setSourceId(request.sourceId());
        }
        if (request.installmentNumber() != null) {
            item.setInstallmentNumber(request.installmentNumber());
        }
        if (request.transactionId() != null) {
            item.setTransactionId(request.transactionId());
        }
        if (request.notes() != null) {
            item.setNotes(optional(request.notes()));
        }
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

    @Transactional(readOnly = true)
    public CreditCardInvoiceItem findOwnedItem(String ownerEmail, Long itemId) {
        return itemRepository.findByIdAndOwnerEmail(itemId, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Item de fatura nao encontrado."));
    }

    private Category resolveCategory(String ownerEmail, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryService.findAvailableCategory(ownerEmail, categoryId);
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("A categoria do item da fatura precisa ser de despesa.");
        }
        return category;
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
