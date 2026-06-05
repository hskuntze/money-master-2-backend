package br.com.kuntzedevprojects.money_master_2.controllers.finance.payment;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentReverseRequest;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;
import jakarta.validation.Valid;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<PaymentResponse>> search(
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(paymentService.search(principal.getName(), cycleId, from, to));
    }

    @PostMapping("/payables/{id}/payments")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<PaymentResponse> registerPayablePayment(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.registerPayablePayment(principal.getName(), id, request));
    }

    @GetMapping("/payables/{id}/payments")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<PaymentResponse>> listPayablePayments(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(paymentService.listPayablePayments(principal.getName(), id));
    }

    @PostMapping("/income-plans/{id}/receipts")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<PaymentResponse> registerIncomeReceipt(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.registerIncomeReceipt(principal.getName(), id, request));
    }

    @GetMapping("/income-plans/{id}/receipts")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<PaymentResponse>> listIncomeReceipts(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(paymentService.listIncomeReceipts(principal.getName(), id));
    }

    @PostMapping("/payments/{id}/reverse")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<PaymentResponse> reverse(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentReverseRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(paymentService.reverse(principal.getName(), id, request));
    }

    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> cancel(@PathVariable Long id, Principal principal) {
        paymentService.cancel(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Pagamento cancelado com sucesso."));
    }
}
