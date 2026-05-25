package br.com.kuntzedevprojects.money_master_2.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.kuntzedevprojects.money_master_2.dtos.user.RoleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserSelfUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.user.UserUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Role;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.exceptions.ResourceNotFoundException;
import br.com.kuntzedevprojects.money_master_2.repositories.RoleRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.UserRepository;

@Service
public class UserService {

    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Path avatarStoragePath;
    private final long maxAvatarSizeBytes;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${money-master.upload.base-dir:uploads}") String uploadBaseDir,
            @Value("${money-master.upload.avatar.max-size-bytes:2097152}") long maxAvatarSizeBytes
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStoragePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize().resolve("avatars");
        this.maxAvatarSizeBytes = maxAvatarSizeBytes;
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

    @Transactional
    public UserResponse updateCurrentProfile(String email, UserSelfUpdateRequest request) {
        User user = findByEmail(email);
        user.setName(request.name());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse uploadCurrentAvatar(String email, MultipartFile file) {
        User user = findByEmail(email);
        validateAvatarFile(file);

        String contentType = normalizedContentType(file.getContentType());
        String extension = extensionFor(contentType);
        String newFileName = user.getId() + "-" + UUID.randomUUID() + extension;
        Path target = avatarStoragePath.resolve(newFileName).normalize();

        try {
            Files.createDirectories(avatarStoragePath);
            if (!target.startsWith(avatarStoragePath)) {
                throw new BusinessException("Nome de arquivo inválido para a foto de perfil.");
            }
            file.transferTo(target);
            deleteAvatarFile(user.getAvatarFileName());
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível salvar a foto de perfil.", ex);
        }

        user.setAvatarFileName(newFileName);
        user.setAvatarContentType(contentType);
        user.setAvatarUpdatedAt(Instant.now());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse deleteCurrentAvatar(String email) {
        User user = findByEmail(email);
        deleteAvatarFile(user.getAvatarFileName());
        user.setAvatarFileName(null);
        user.setAvatarContentType(null);
        user.setAvatarUpdatedAt(null);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AvatarResource loadAvatar(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (user.getAvatarFileName() == null || user.getAvatarFileName().isBlank()) {
            throw new ResourceNotFoundException("Foto de perfil não encontrada.");
        }

        Path avatarPath = avatarStoragePath.resolve(user.getAvatarFileName()).normalize();
        if (!avatarPath.startsWith(avatarStoragePath) || !Files.exists(avatarPath)) {
            throw new ResourceNotFoundException("Foto de perfil não encontrada.");
        }

        try {
            Resource resource = new UrlResource(avatarPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Foto de perfil não encontrada.");
            }
            return new AvatarResource(resource, user.getAvatarContentType(), user.getAvatarUpdatedAt());
        } catch (MalformedURLException ex) {
            throw new BusinessException("Não foi possível carregar a foto de perfil.");
        }
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

    private User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
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

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Selecione uma imagem para a foto de perfil.");
        }
        if (file.getSize() > maxAvatarSizeBytes) {
            throw new BusinessException("A foto de perfil deve ter no máximo " + (maxAvatarSizeBytes / 1024 / 1024) + " MB.");
        }
        String contentType = normalizedContentType(file.getContentType());
        if (!ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("A foto de perfil deve ser uma imagem JPG, PNG ou WEBP.");
        }
    }

    private String normalizedContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BusinessException("Tipo de imagem não suportado.");
        };
    }

    private void deleteAvatarFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Path filePath = avatarStoragePath.resolve(fileName).normalize();
        if (!filePath.startsWith(avatarStoragePath)) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível remover a foto de perfil anterior.", ex);
        }
    }

    public record AvatarResource(Resource resource, String contentType, Instant updatedAt) {
    }
}
