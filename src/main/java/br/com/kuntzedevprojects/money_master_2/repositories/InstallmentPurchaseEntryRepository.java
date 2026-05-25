package br.com.kuntzedevprojects.money_master_2.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchaseEntry;

public interface InstallmentPurchaseEntryRepository extends JpaRepository<InstallmentPurchaseEntry, Long> {

    @Query("""
            select e
            from InstallmentPurchaseEntry e
            join fetch e.purchase p
            left join fetch p.account a
            left join fetch p.category c
            where lower(e.owner.email) = lower(:ownerEmail)
              and e.dueDate between :startDate and :endDate
              and e.monthlyPlanItem is null
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus.ACTIVE
            order by e.dueDate asc, e.installmentNumber asc
            """)
    List<InstallmentPurchaseEntry> findPendingWithoutPlanItemForPeriod(
            @Param("ownerEmail") String ownerEmail,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
