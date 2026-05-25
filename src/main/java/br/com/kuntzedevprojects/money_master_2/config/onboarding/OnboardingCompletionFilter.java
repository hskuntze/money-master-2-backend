package br.com.kuntzedevprojects.money_master_2.config.onboarding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.kuntzedevprojects.money_master_2.repositories.UserFinancialProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OnboardingCompletionFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/auth",
            "/themes",
            "/users/me",
            "/onboarding",
            "/financial-profile",
            "/actuator"
    );

    private final UserFinancialProfileRepository profileRepository;

    public OnboardingCompletionFilter(UserFinancialProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (isAllowed(path) || hasAuthority(authentication, "ROLE_ADMIN")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean completed = profileRepository.findByOwnerEmailIgnoreCase(authentication.getName())
                .map(profile -> profile.isOnboardingCompleted())
                .orElse(false);
        if (completed) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(428);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Finalize a configuração inicial antes de acessar esta funcionalidade.\",\"code\":\"ONBOARDING_REQUIRED\"}");
    }

    private boolean isAllowed(String path) {
        return ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
