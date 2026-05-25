package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.UserFinancialProfile;

public interface UserFinancialProfileRepository extends JpaRepository<UserFinancialProfile, Long> {

    Optional<UserFinancialProfile> findByOwnerEmailIgnoreCase(String ownerEmail);
}
