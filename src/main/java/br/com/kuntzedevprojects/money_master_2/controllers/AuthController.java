package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthLoginRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthRefreshRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthRegisterRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.AuthResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserResponse;
import br.com.kuntzedevprojects.money_master_2.services.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/confirm-email")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> confirmEmail(@RequestParam String token) {
        authService.confirmEmail(token);
        return ResponseEntity.ok(new MessageResponse("E-mail confirmado com sucesso."));
    }

    @PostMapping("/resend-confirmation")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> resendConfirmation(@RequestParam String email) {
        authService.resendConfirmation(email);
        return ResponseEntity.ok(new MessageResponse("E-mail de confirmação reenviado."));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me(Principal principal) {
        return ResponseEntity.ok(authService.me(principal.getName()));
    }
}
