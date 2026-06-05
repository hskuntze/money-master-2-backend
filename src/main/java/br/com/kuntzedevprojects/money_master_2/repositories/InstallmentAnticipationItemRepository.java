package br.com.kuntzedevprojects.money_master_2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipationItem;

public interface InstallmentAnticipationItemRepository extends JpaRepository<InstallmentAnticipationItem, Long> {
}
