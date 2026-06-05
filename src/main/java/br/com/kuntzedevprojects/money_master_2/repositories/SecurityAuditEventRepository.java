package br.com.kuntzedevprojects.money_master_2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.kuntzedevprojects.money_master_2.entities.SecurityAuditEvent;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long>, JpaSpecificationExecutor<SecurityAuditEvent> {
}
