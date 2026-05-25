package br.com.kuntzedevprojects.money_master_2.dtos.user;

import java.time.Instant;
import java.util.Set;

import br.com.kuntzedevprojects.money_master_2.entities.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        boolean enabled,
        boolean emailVerified,
        boolean accountNonLocked,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt,
        String avatarUrl,
        Instant avatarUpdatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isAccountNonLocked(),
                user.roleNames(),
                user.permissionNames(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt(),
                avatarUrl(user),
                user.getAvatarUpdatedAt()
        );
    }

    private static String avatarUrl(User user) {
        if (user.getId() == null || user.getAvatarFileName() == null || user.getAvatarFileName().isBlank()) {
            return null;
        }
        String version = user.getAvatarUpdatedAt() == null ? "" : "?v=" + user.getAvatarUpdatedAt().toEpochMilli();
        return "/users/" + user.getId() + "/avatar" + version;
    }
}
