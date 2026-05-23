package br.com.kuntzedevprojects.money_master_2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.theme.ThemeResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.theme.ThemeUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Theme;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.ThemeRepository;

@Service
public class ThemeService {

    private final ThemeRepository themeRepository;

    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @Transactional(readOnly = true)
    public ThemeResponse getActiveTheme() {
        return ThemeResponse.from(findActiveTheme());
    }

    @Transactional
    public ThemeResponse updateActiveTheme(ThemeUpdateRequest request) {
        Theme theme = findActiveTheme();
        theme.setAppName(request.appName());
        theme.setLogoUrl(request.logoUrl());
        theme.setFaviconUrl(request.faviconUrl());
        theme.setPrimaryColor(request.primaryColor());
        theme.setSecondaryColor(request.secondaryColor());
        theme.setAccentColor(request.accentColor());
        theme.setBackgroundColor(request.backgroundColor());
        theme.setTextColor(request.textColor());
        theme.setCardColor(request.cardColor());
        theme.setLoginTitle(request.loginTitle());
        theme.setLoginSubtitle(request.loginSubtitle());
        return ThemeResponse.from(theme);
    }

    private Theme findActiveTheme() {
        return themeRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Tema ativo não encontrado."));
    }
}
