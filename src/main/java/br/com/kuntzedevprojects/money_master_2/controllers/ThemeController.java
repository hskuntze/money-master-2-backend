package br.com.kuntzedevprojects.money_master_2.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.theme.ThemeResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.theme.ThemeUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.ThemeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/themes")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping("/active")
    public ResponseEntity<ThemeResponse> getActiveTheme() {
        return ResponseEntity.ok(themeService.getActiveTheme());
    }

    @PutMapping("/active")
    @PreAuthorize("hasAuthority('THEME_MANAGE')")
    public ResponseEntity<ThemeResponse> updateActiveTheme(@Valid @RequestBody ThemeUpdateRequest request) {
        return ResponseEntity.ok(themeService.updateActiveTheme(request));
    }
}
