package br.com.kuntzedevprojects.money_master_2.config.bootstrap;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.config.properties.BootstrapAdminProperties;
import br.com.kuntzedevprojects.money_master_2.entities.Role;
import br.com.kuntzedevprojects.money_master_2.entities.Theme;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.repositories.RoleRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.ThemeRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;
import br.com.kuntzedevprojects.money_master_2.services.PasswordPolicyService;

@Component
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ThemeRepository themeRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    public DataBootstrap(
            BootstrapAdminProperties properties,
            UserRepository userRepository,
            RoleRepository roleRepository,
            ThemeRepository themeRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.themeRepository = themeRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDefaultTheme();
        ensureAdminUser();
    }

    private void ensureDefaultTheme() {
        if (themeRepository.existsByActiveTrue()) {
            return;
        }
        Theme theme = Theme.defaultTheme();
        themeRepository.save(theme);
    }

    private void ensureAdminUser() {
        if (!properties.isEnabled()) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(properties.getEmail())) {
            return;
        }

        passwordPolicyService.validate(properties.getPassword());

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN não encontrada. Verifique a migration V2."));

        User admin = new User();
        admin.setName(properties.getName());
        admin.setEmail(properties.getEmail());
        admin.setPassword(passwordEncoder.encode(properties.getPassword()));
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.setAccountNonLocked(true);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);
        log.warn("Usuário administrador bootstrap criado: {}. Altere app.bootstrap.admin.password por variável de ambiente.", properties.getEmail());
    }
}
