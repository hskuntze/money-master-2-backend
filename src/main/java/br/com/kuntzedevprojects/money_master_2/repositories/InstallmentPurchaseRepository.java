package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;

public interface InstallmentPurchaseRepository extends JpaRepository<InstallmentPurchase, Long> {

    @Query("""
            select distinct p
            from InstallmentPurchase p
            left join fetch p.category c
            left join fetch p.account a
            left join fetch p.creditCard cc
            left join fetch p.firstInvoice fi
            left join fetch p.entries e
            left join fetch e.financialPeriod fp
            left join fetch e.monthlyPlanItem mpi
            left join fetch e.invoiceItem invoiceItem
            left join fetch invoiceItem.invoice invoice
            left join fetch mpi.parentItem parentItem
            where lower(p.owner.email) = lower(:ownerEmail)
            order by p.firstDueDate desc, p.id desc
            """)
    List<InstallmentPurchase> findByOwnerEmailWithEntries(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select distinct p
            from InstallmentPurchase p
            left join fetch p.category c
            left join fetch p.account a
            left join fetch p.creditCard cc
            left join fetch p.firstInvoice fi
            left join fetch p.entries e
            left join fetch e.financialPeriod fp
            left join fetch e.monthlyPlanItem mpi
            left join fetch e.invoiceItem invoiceItem
            left join fetch invoiceItem.invoice invoice
            left join fetch mpi.parentItem parentItem
            where p.id = :id
              and lower(p.owner.email) = lower(:ownerEmail)
            """)
    Optional<InstallmentPurchase> findByIdAndOwnerEmailWithEntries(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);
}
