package br.com.kuntzedevprojects.money_master_2.dtos.theme;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThemeUpdateRequest(
        @NotBlank @Size(max = 120) String appName,
        @Size(max = 500) String logoUrl,
        @NotBlank @Size(max = 20) String primaryColor,
        @NotBlank @Size(max = 20) String secondaryColor,
        @NotBlank @Size(max = 20) String accentColor,
        @NotBlank @Size(max = 20) String backgroundColor,
        @NotBlank @Size(max = 20) String textColor,
        @NotBlank @Size(max = 20) String cardColor,
        @Size(max = 140) String loginTitle,
        @Size(max = 255) String loginSubtitle
) {
}
