package br.com.kuntzedevprojects.money_master_2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.DebtInstallment;

public interface DebtInstallmentRepository extends JpaRepository<DebtInstallment, Long> {
}
