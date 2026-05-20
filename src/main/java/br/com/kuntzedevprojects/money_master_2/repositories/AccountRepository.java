package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByOwnerEmailIgnoreCaseOrderByNameAsc(String ownerEmail);

    List<Account> findByOwnerEmailIgnoreCaseAndActiveTrueOrderByNameAsc(String ownerEmail);

    Optional<Account> findByIdAndOwnerEmailIgnoreCase(Long id, String ownerEmail);

    Optional<Account> findByOwnerEmailIgnoreCaseAndNameIgnoreCase(String ownerEmail, String name);

    boolean existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(String ownerEmail, String name);
}
