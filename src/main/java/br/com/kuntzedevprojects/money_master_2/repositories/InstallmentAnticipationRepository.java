package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.InstallmentAnticipation;

public interface InstallmentAnticipationRepository extends JpaRepository<InstallmentAnticipation, Long> {

    @Query("""
            select distinct a
            from InstallmentAnticipation a
            join fetch a.owner owner
            join fetch a.purchase purchase
            join fetch a.cycle cycle
            left join fetch a.creditCard card
            left join fetch a.targetInvoice invoice
            left join fetch a.items items
            left join fetch items.installment installment
            where lower(owner.email) = lower(:ownerEmail)
              and purchase.id = :purchaseId
            order by a.anticipationDate desc, a.id desc
            """)
    List<InstallmentAnticipation> findByPurchaseIdAndOwnerEmail(@Param("ownerEmail") String ownerEmail, @Param("purchaseId") Long purchaseId);

    @Query("""
            select distinct a
            from InstallmentAnticipation a
            join fetch a.owner owner
            join fetch a.purchase purchase
            join fetch a.cycle cycle
            left join fetch a.creditCard card
            left join fetch a.targetInvoice invoice
            left join fetch a.items items
            left join fetch items.installment installment
            where lower(owner.email) = lower(:ownerEmail)
              and a.id = :id
            """)
    Optional<InstallmentAnticipation> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);
}
