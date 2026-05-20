package br.com.kuntzedevprojects.money_master_2.dtos.user;

import java.util.Set;
import java.util.stream.Collectors;

import br.com.kuntzedevprojects.money_master_2.entities.Role;

public record RoleResponse(Long id, String name, String description, Set<String> permissions) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream().map(permission -> permission.getName()).collect(Collectors.toSet())
        );
    }
}
