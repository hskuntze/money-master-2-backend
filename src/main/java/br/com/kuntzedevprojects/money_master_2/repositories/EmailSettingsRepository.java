package br.com.kuntzedevprojects.money_master_2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.EmailSettings;

public interface EmailSettingsRepository extends JpaRepository<EmailSettings, Long> {
}
