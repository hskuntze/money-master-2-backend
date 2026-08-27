package br.com.kuntzedevprojects.money_master_2.controllers.finance.investment;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentProductUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentProductService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/investments")
public class InvestmentProductController {

    private final InvestmentProductService investmentProductService;

    public InvestmentProductController(InvestmentProductService investmentProductService) {
        this.investmentProductService = investmentProductService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<InvestmentProductResponse>> list(Principal principal) {
        return ResponseEntity.ok(investmentProductService.list(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<InvestmentProductResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(investmentProductService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InvestmentProductResponse> create(
            @Valid @RequestBody InvestmentProductCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentProductService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InvestmentProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentProductUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(investmentProductService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, Principal principal) {
        boolean deleted = investmentProductService.delete(principal.getName(), id);
        String message = deleted
                ? "Produto financeiro excluído com sucesso."
                : "Produto financeiro arquivado. O histórico de movimentações foi preservado.";
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<InvestmentMovementResponse>> movements(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(investmentProductService.movements(principal.getName(), id));
    }

    @PostMapping("/{id}/contributions")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InvestmentMovementResponse> contribute(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentMovementRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentProductService.contribute(principal.getName(), id, request));
    }

    @PostMapping("/{id}/withdrawals")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InvestmentMovementResponse> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentMovementRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentProductService.withdraw(principal.getName(), id, request));
    }

    @PostMapping("/{id}/yields")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InvestmentMovementResponse> registerYield(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentMovementRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentProductService.registerYield(principal.getName(), id, request));
    }
}
