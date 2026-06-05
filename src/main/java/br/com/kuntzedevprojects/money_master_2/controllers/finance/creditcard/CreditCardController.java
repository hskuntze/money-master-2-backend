package br.com.kuntzedevprojects.money_master_2.controllers.finance.creditcard;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoiceItemService;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoicePaymentService;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoiceService;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardService;
import jakarta.validation.Valid;

@RestController
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardInvoiceService invoiceService;
    private final CreditCardInvoiceItemService itemService;
    private final CreditCardInvoicePaymentService paymentService;

    public CreditCardController(
            CreditCardService creditCardService,
            CreditCardInvoiceService invoiceService,
            CreditCardInvoiceItemService itemService,
            CreditCardInvoicePaymentService paymentService
    ) {
        this.creditCardService = creditCardService;
        this.invoiceService = invoiceService;
        this.itemService = itemService;
        this.paymentService = paymentService;
    }

    @GetMapping("/credit-cards")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<CreditCardResponse>> listCards(Principal principal) {
        return ResponseEntity.ok(creditCardService.list(principal.getName()));
    }

    @PostMapping("/credit-cards")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardResponse> createCard(
            @Valid @RequestBody CreditCardCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.create(principal.getName(), request));
    }

    @GetMapping("/credit-cards/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<CreditCardResponse> getCard(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(creditCardService.get(principal.getName(), id));
    }

    @PutMapping("/credit-cards/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardResponse> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CreditCardUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(creditCardService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/credit-cards/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> deleteCard(@PathVariable Long id, Principal principal) {
        creditCardService.deactivate(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Cartao desativado com sucesso."));
    }

    @GetMapping("/credit-cards/{cardId}/invoices")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<CreditCardInvoiceResponse>> listCardInvoices(
            @PathVariable Long cardId,
            @RequestParam(required = false) Long cycleId,
            Principal principal
    ) {
        return ResponseEntity.ok(invoiceService.search(principal.getName(), cardId, cycleId));
    }

    @PostMapping("/credit-cards/{cardId}/invoices")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceResponse> createInvoice(
            @PathVariable Long cardId,
            @Valid @RequestBody CreditCardInvoiceCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(principal.getName(), cardId, request));
    }

    @GetMapping("/credit-card-invoices")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<CreditCardInvoiceResponse>> searchInvoices(
            @RequestParam(required = false) Long cardId,
            @RequestParam(required = false) Long cycleId,
            Principal principal
    ) {
        return ResponseEntity.ok(invoiceService.search(principal.getName(), cardId, cycleId));
    }

    @GetMapping("/credit-card-invoices/{invoiceId}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<CreditCardInvoiceResponse> getInvoice(@PathVariable Long invoiceId, Principal principal) {
        return ResponseEntity.ok(invoiceService.get(principal.getName(), invoiceId));
    }

    @PutMapping("/credit-card-invoices/{invoiceId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceResponse> updateInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody CreditCardInvoiceUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(invoiceService.update(principal.getName(), invoiceId, request));
    }

    @PostMapping("/credit-card-invoices/{invoiceId}/close")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceResponse> closeInvoice(@PathVariable Long invoiceId, Principal principal) {
        return ResponseEntity.ok(invoiceService.close(principal.getName(), invoiceId));
    }

    @PostMapping("/credit-card-invoices/{invoiceId}/reopen")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceResponse> reopenInvoice(@PathVariable Long invoiceId, Principal principal) {
        return ResponseEntity.ok(invoiceService.reopen(principal.getName(), invoiceId));
    }

    @PostMapping("/credit-card-invoices/{invoiceId}/payments")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceResponse> payInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody(required = false) PaymentRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(paymentService.pay(principal.getName(), invoiceId, request));
    }

    @GetMapping("/credit-card-invoices/{invoiceId}/items")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<CreditCardInvoiceItemResponse>> listItems(@PathVariable Long invoiceId, Principal principal) {
        return ResponseEntity.ok(itemService.list(principal.getName(), invoiceId));
    }

    @PostMapping("/credit-card-invoices/{invoiceId}/items")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceItemResponse> createItem(
            @PathVariable Long invoiceId,
            @Valid @RequestBody CreditCardInvoiceItemCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(principal.getName(), invoiceId, request));
    }

    @PutMapping("/credit-card-invoice-items/{itemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CreditCardInvoiceItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody CreditCardInvoiceItemUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(itemService.update(principal.getName(), itemId, request));
    }

    @DeleteMapping("/credit-card-invoice-items/{itemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable Long itemId, Principal principal) {
        itemService.delete(principal.getName(), itemId);
        return ResponseEntity.ok(new MessageResponse("Item removido da fatura."));
    }
}
