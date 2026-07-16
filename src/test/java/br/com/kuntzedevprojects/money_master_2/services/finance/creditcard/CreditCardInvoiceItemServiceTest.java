package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;

class CreditCardInvoiceItemServiceTest {

    private final CreditCardInvoiceItemRepository itemRepository = mock(CreditCardInvoiceItemRepository.class);
    private final CreditCardInvoiceService invoiceService = mock(CreditCardInvoiceService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final FinancialTransactionService transactionService = mock(FinancialTransactionService.class);

    private final CreditCardInvoiceItemService service = new CreditCardInvoiceItemService(
            itemRepository,
            invoiceService,
            currentUserService,
            categoryService,
            transactionService
    );

    @Test
    void shouldCreateCardPurchaseItemWithStructuralTransactionLink() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        Category category = category();
        FinancialTransaction transaction = transaction(owner, category);
        when(invoiceService.findOwnedInvoice(owner.getEmail(), invoice.getId())).thenReturn(invoice);
        when(currentUserService.findUserByEmail(owner.getEmail())).thenReturn(owner);
        when(transactionService.findOwnedTransaction(owner.getEmail(), transaction.getId())).thenReturn(transaction);
        when(itemRepository.existsByTransactionIdExcludingItem(transaction.getId(), null)).thenReturn(false);
        when(itemRepository.save(any(CreditCardInvoiceItem.class))).thenAnswer(invocation -> {
            CreditCardInvoiceItem item = invocation.getArgument(0);
            item.setId(99L);
            return item;
        });

        CreditCardInvoiceItemCreateRequest request = new CreditCardInvoiceItemCreateRequest(
                null,
                "Mercado",
                new BigDecimal("123.45"),
                LocalDate.of(2026, 7, 10),
                null,
                null,
                null,
                null,
                transaction.getId(),
                null
        );

        CreditCardInvoiceItemResponse response = service.create(owner.getEmail(), invoice.getId(), request);

        ArgumentCaptor<CreditCardInvoiceItem> captor = ArgumentCaptor.forClass(CreditCardInvoiceItem.class);
        verify(itemRepository).save(captor.capture());
        CreditCardInvoiceItem saved = captor.getValue();
        assertThat(saved.getSourceType()).isEqualTo(CreditCardInvoiceItemSourceType.CARD_PURCHASE);
        assertThat(saved.getTransaction()).isSameAs(transaction);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(response.transactionId()).isEqualTo(transaction.getId());
        verify(transactionService).markAsCreditCardInvoiceItem(owner.getEmail(), transaction);
        verify(invoiceService).syncInvoiceTotals(owner.getEmail(), invoice);
    }

    @Test
    void shouldRejectTransactionAlreadyLinkedToAnotherInvoiceItem() {
        User owner = owner();
        CreditCardInvoice invoice = invoice();
        when(invoiceService.findOwnedInvoice(owner.getEmail(), invoice.getId())).thenReturn(invoice);
        when(itemRepository.existsByTransactionIdExcludingItem(55L, null)).thenReturn(true);

        CreditCardInvoiceItemCreateRequest request = new CreditCardInvoiceItemCreateRequest(
                null,
                "Mercado",
                new BigDecimal("123.45"),
                LocalDate.of(2026, 7, 10),
                null,
                CreditCardInvoiceItemSourceType.CARD_PURCHASE,
                null,
                null,
                55L,
                null
        );

        assertThatThrownBy(() -> service.create(owner.getEmail(), invoice.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ja esta vinculada");
    }

    private User owner() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");
        owner.setName("User");
        return owner;
    }

    private CreditCardInvoice invoice() {
        CreditCardInvoice invoice = new CreditCardInvoice();
        invoice.setId(10L);
        return invoice;
    }

    private Category category() {
        Category category = new Category();
        category.setId(20L);
        category.setName("Mercado");
        category.setType(TransactionType.EXPENSE);
        return category;
    }

    private FinancialTransaction transaction(User owner, Category category) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setId(55L);
        transaction.setOwner(owner);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("123.45"));
        transaction.setOccurredOn(LocalDate.of(2026, 7, 10));
        transaction.setCategory(category);
        return transaction;
    }
}
