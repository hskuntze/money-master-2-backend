package br.com.kuntzedevprojects.money_master_2.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @Query("""
            select e
            from InstallmentPurchaseEntry e
            join fetch e.purchase p
            left join fetch e.financialPeriod fp
            left join fetch e.monthlyPlanItem mpi
            left join fetch mpi.parentItem parentItem
            left join fetch e.paidBy paidBy
            where e.id = :id
              and lower(e.owner.email) = lower(:ownerEmail)
            """)
    Optional<InstallmentPurchaseEntry> findByIdAndOwnerEmailWithRelations(
            @Param("id") Long id,
            @Param("ownerEmail") String ownerEmail
    );

    @Query("""
            select e
            from InstallmentPurchaseEntry e
            join fetch e.purchase p
            left join fetch e.financialPeriod fp
            left join fetch e.monthlyPlanItem mpi
            left join fetch mpi.parentItem parentItem
            left join fetch e.paidBy paidBy
            where lower(e.owner.email) = lower(:ownerEmail)
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.InstallmentPurchaseStatus.ACTIVE
              and e.status <> br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus.CANCELED
            order by e.dueDate asc, e.installmentNumber asc
            """)
    List<InstallmentPurchaseEntry> findActiveEntriesByOwnerEmail(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select e
            from InstallmentPurchaseEntry e
            join fetch e.purchase p
            left join fetch e.financialPeriod fp
            left join fetch e.monthlyPlanItem mpi
            left join fetch mpi.parentItem parentItem
            left join fetch e.paidBy paidBy
            where lower(e.owner.email) = lower(:ownerEmail)
              and mpi.id in :monthlyPlanItemIds
            """)
    List<InstallmentPurchaseEntry> findByMonthlyPlanItemIdsAndOwnerEmail(
            @Param("monthlyPlanItemIds") List<Long> monthlyPlanItemIds,
            @Param("ownerEmail") String ownerEmail
    );

}
