package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByName(String name);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findByNameIn(Collection<String> names);
}
