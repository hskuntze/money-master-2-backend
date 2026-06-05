package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthLoginRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthRefreshRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthRegisterRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Role;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.RoleRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private static final String GENERIC_AUTHENTICATION_MESSAGE = "Não foi possível autenticar com as credenciais informadas.";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordPolicyService passwordPolicyService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService securityAuditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            EmailConfirmationService emailConfirmationService,
            PasswordPolicyService passwordPolicyService,
            LoginAttemptService loginAttemptService,
            SecurityAuditService securityAuditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailConfirmationService = emailConfirmationService;
        this.passwordPolicyService = passwordPolicyService;
        this.loginAttemptService = loginAttemptService;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public UserResponse register(AuthRegisterRequest request, HttpServletRequest servletRequest) {
        passwordPolicyService.validate(request.password());
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            securityAuditService.record("REGISTER_DUPLICATE_EMAIL", email, false, servletRequest,
                    "Tentativa de cadastro com e-mail já existente.");
            throw new BusinessException("Não foi possível concluir o cadastro com os dados informados.");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BusinessException("Perfil padrão ROLE_USER não encontrado."));

        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setAccountNonLocked(true);
        user.setRoles(Set.of(defaultRole));

        User saved = userRepository.save(user);
        emailConfirmationService.createAndSend(saved);
        securityAuditService.record("REGISTER_SUCCESS", email, true, servletRequest,
                "Cadastro criado; aguardando confirmação de e-mail.");
        return UserResponse.from(saved);
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.email());
        loginAttemptService.assertAllowed(email, servletRequest);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(email, servletRequest);
            throw new BadCredentialsException(GENERIC_AUTHENTICATION_MESSAGE);
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException(GENERIC_AUTHENTICATION_MESSAGE));

        user.setLastLoginAt(Instant.now());
        loginAttemptService.recordSuccess(email, servletRequest);

        JwtService.IssuedToken accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        return new AuthResponse("Bearer", accessToken.value(), accessToken.expiresAt(), refreshToken, UserResponse.from(user));
    }

    @Transactional
    public AuthResponse refresh(AuthRefreshRequest request, HttpServletRequest servletRequest) {
        User user = refreshTokenService.consume(request.refreshToken());
        JwtService.IssuedToken accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        securityAuditService.record("REFRESH_TOKEN_ROTATED", user.getEmail(), true, servletRequest,
                "Refresh token consumido e rotacionado.");
        return new AuthResponse("Bearer", accessToken.value(), accessToken.expiresAt(), refreshToken, UserResponse.from(user));
    }

    @Transactional
    public void logout(String refreshToken, String principal, HttpServletRequest servletRequest) {
        refreshTokenService.revoke(refreshToken);
        securityAuditService.record("LOGOUT", principal, true, servletRequest, "Logout solicitado pelo usuário.");
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("Usuário autenticado não encontrado."));
        return UserResponse.from(user);
    }

    @Transactional
    public void confirmEmail(String token) {
        emailConfirmationService.confirm(token);
    }

    @Transactional
    public void resendConfirmation(String email, HttpServletRequest servletRequest) {
        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(emailConfirmationService::createAndSend);
        securityAuditService.record("EMAIL_CONFIRMATION_RESEND_REQUESTED", normalizedEmail, true, servletRequest,
                "Solicitação pública de reenvio de confirmação processada com resposta genérica.");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
