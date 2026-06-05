package br.com.kuntzedevprojects.money_master_2.services.finance.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard.CreditCardInvoiceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentResponse;
import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentSource;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;

@Service
public class CreditCardInvoicePaymentService {

    private final CreditCardInvoiceService invoiceService;
    private final PaymentService paymentService;

    public CreditCardInvoicePaymentService(CreditCardInvoiceService invoiceService, PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @Transactional
    public CreditCardInvoiceResponse pay(String ownerEmail, Long invoiceId, PaymentRequest request) {
        CreditCardInvoice invoice = invoiceService.findOwnedInvoice(ownerEmail, invoiceId);
        if (invoice.getMonthlyPayable() == null) {
            throw new BusinessException("A fatura ainda nao possui conta mensal vinculada.");
        }
        BigDecimal amount = request == null ? null : request.amount();
        LocalDate paymentDate = request == null ? null : request.paymentDate();
        PaymentMethod method = request == null ? null : request.method();
        Long accountId = request == null ? null : request.accountId();
        Long categoryId = request == null ? null : request.categoryId();
        String notes = request == null ? null : request.notes();
        PaymentRequest paymentRequest = new PaymentRequest(
                request == null ? null : request.transactionId(),
                accountId,
                categoryId,
                amount,
                paymentDate,
                method,
                PaymentSource.INVOICE_PAYMENT,
                request == null ? true : request.createTransaction(),
                notes
        );
        PaymentResponse ignored = paymentService.registerPayablePayment(ownerEmail, invoice.getMonthlyPayable().getId(), paymentRequest);
        invoiceService.refreshPaymentState(invoice);
        return invoiceService.get(ownerEmail, invoiceId);
    }
}
