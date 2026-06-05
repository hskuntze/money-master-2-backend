package br.com.kuntzedevprojects.money_master_2.services;

import org.springframework.stereotype.Service;

import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;

@Service
public class PasswordPolicyService {

    private final SecurityHardeningProperties.PasswordPolicy policy;

    public PasswordPolicyService(SecurityHardeningProperties properties) {
        this.policy = properties.getPasswordPolicy();
    }

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("A senha é obrigatória.");
        }
        if (password.length() < policy.getMinLength()) {
            throw new BusinessException("A senha deve ter pelo menos " + policy.getMinLength() + " caracteres.");
        }
        if (password.length() > policy.getMaxLength()) {
            throw new BusinessException("A senha deve ter no máximo " + policy.getMaxLength() + " caracteres.");
        }
        if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new BusinessException("A senha deve conter pelo menos uma letra maiúscula.");
        }
        if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new BusinessException("A senha deve conter pelo menos uma letra minúscula.");
        }
        if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessException("A senha deve conter pelo menos um número.");
        }
        if (policy.isRequireSpecial() && password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new BusinessException("A senha deve conter pelo menos um caractere especial.");
        }
    }
}
