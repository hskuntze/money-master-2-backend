package br.com.kuntzedevprojects.money_master_2.dtos.theme;

import br.com.kuntzedevprojects.money_master_2.entities.Theme;

public record ThemeResponse(
        Long id,
        String appName,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String textColor,
        String cardColor,
        String loginTitle,
        String loginSubtitle
) {
    public static ThemeResponse from(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getAppName(),
                theme.getLogoUrl(),
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getAccentColor(),
                theme.getBackgroundColor(),
                theme.getTextColor(),
                theme.getCardColor(),
                theme.getLoginTitle(),
                theme.getLoginSubtitle()
        );
    }
}
