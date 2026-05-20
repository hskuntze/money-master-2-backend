package br.com.kuntzedevprojects.money_master_2.controllers;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<CategoryResponse>> list(
            @RequestParam(required = false) TransactionType type,
            Principal principal
    ) {
        return ResponseEntity.ok(categoryService.listAvailable(principal.getName(), type));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<CategoryResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(categoryService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(categoryService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> deactivate(@PathVariable Long id, Principal principal) {
        categoryService.deactivate(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Categoria desativada com sucesso."));
    }
}
