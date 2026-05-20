package br.com.kuntzedevprojects.money_master_2.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.kuntzedevprojects.money_master_2.dtos.user.RoleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Role;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.RoleRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Já existe usuário cadastrado com este e-mail.");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(Boolean.TRUE.equals(request.enabled()));
        user.setEmailVerified(Boolean.TRUE.equals(request.enabled()));
        user.setAccountNonLocked(true);
        user.setRoles(resolveRoles(request.roles(), Set.of("ROLE_USER")));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
            if (request.enabled()) {
                user.setEmailVerified(true);
            }
        }
        if (request.accountNonLocked() != null) {
            user.setAccountNonLocked(request.accountNonLocked());
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(resolveRoles(request.roles(), Set.of("ROLE_USER")));
        }

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(RoleResponse::from)
                .toList();
    }

    private User findUser(Long id) {
        return userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private Set<Role> resolveRoles(Set<String> requestedRoles, Set<String> fallback) {
        Set<String> normalized = (requestedRoles == null || requestedRoles.isEmpty() ? fallback : requestedRoles)
                .stream()
                .map(this::normalizeRoleName)
                .collect(Collectors.toSet());

        List<Role> roles = roleRepository.findByNameIn(normalized);
        if (roles.size() != normalized.size()) {
            throw new BusinessException("Uma ou mais roles informadas não existem: " + normalized);
        }
        return Set.copyOf(roles);
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
