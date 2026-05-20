package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailConfirmationService emailConfirmationService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            EmailConfirmationService emailConfirmationService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailConfirmationService = emailConfirmationService;
    }

    @Transactional
    public UserResponse register(AuthRegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Já existe usuário cadastrado com este e-mail.");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BusinessException("Perfil padrão ROLE_USER não encontrado."));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setAccountNonLocked(true);
        user.setRoles(Set.of(defaultRole));

        User saved = userRepository.save(user);
        emailConfirmationService.createAndSend(saved);
        return UserResponse.from(saved);
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        user.setLastLoginAt(Instant.now());
        JwtService.IssuedToken accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        return new AuthResponse("Bearer", accessToken.value(), accessToken.expiresAt(), refreshToken, UserResponse.from(user));
    }

    @Transactional
    public AuthResponse refresh(AuthRefreshRequest request) {
        User user = refreshTokenService.consume(request.refreshToken());
        JwtService.IssuedToken accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse("Bearer", accessToken.value(), accessToken.expiresAt(), refreshToken, UserResponse.from(user));
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
    public void resendConfirmation(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        if (user.isEmailVerified()) {
            throw new BusinessException("Este e-mail já foi confirmado.");
        }

        emailConfirmationService.createAndSend(user);
    }
}
