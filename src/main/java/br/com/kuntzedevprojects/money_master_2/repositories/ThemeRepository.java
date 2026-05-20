package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.Theme;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findFirstByActiveTrueOrderByIdAsc();
    boolean existsByActiveTrue();
}
