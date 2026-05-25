package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.reference.FinancialReferenceRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.reference.FinancialReferenceResponse;
import br.com.kuntzedevprojects.money_master_2.services.FinancialReferenceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/financial-references")
public class FinancialReferenceController {

	private final FinancialReferenceService referenceService;

	public FinancialReferenceController(FinancialReferenceService referenceService) {
		this.referenceService = referenceService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('FINANCE_READ')")
	public ResponseEntity<List<FinancialReferenceResponse>> list(
			@RequestParam(defaultValue = "false") boolean activeOnly, Principal principal) {
		return ResponseEntity.ok(referenceService.list(principal.getName(), activeOnly));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('FINANCE_MANAGE')")
	public ResponseEntity<FinancialReferenceResponse> create(@Valid @RequestBody FinancialReferenceRequest request,
			Principal principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.create(principal.getName(), request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('FINANCE_MANAGE')")
	public ResponseEntity<FinancialReferenceResponse> update(@PathVariable Long id,
			@Valid @RequestBody FinancialReferenceRequest request, Principal principal) {
		return ResponseEntity.ok(referenceService.update(principal.getName(), id, request));
	}
}
