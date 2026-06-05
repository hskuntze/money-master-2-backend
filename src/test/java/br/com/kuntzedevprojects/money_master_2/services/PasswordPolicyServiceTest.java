package br.com.kuntzedevprojects.money_master_2.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService(new SecurityHardeningProperties());

    @Test
    void shouldRejectWeakPassword() {
        assertThrows(BusinessException.class, () -> service.validate("123456"));
        assertThrows(BusinessException.class, () -> service.validate("senhafraca"));
        assertThrows(BusinessException.class, () -> service.validate("SenhaFraca1"));
    }

    @Test
    void shouldAcceptPasswordThatMatchesPolicy() {
        assertDoesNotThrow(() -> service.validate("SenhaForte@123"));
    }
}
