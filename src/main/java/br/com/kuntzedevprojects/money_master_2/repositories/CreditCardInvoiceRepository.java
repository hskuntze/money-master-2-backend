package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoice;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {

    @Query("""
            select i
            from CreditCardInvoice i
            join fetch i.owner owner
            join fetch i.creditCard card
            join fetch i.cycle cycle
            left join fetch i.monthlyPayable payable
            where i.id = :id
              and lower(owner.email) = lower(:ownerEmail)
            """)
    Optional<CreditCardInvoice> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select i
            from CreditCardInvoice i
            join fetch i.owner owner
            join fetch i.creditCard card
            join fetch i.cycle cycle
            left join fetch i.monthlyPayable payable
            where lower(owner.email) = lower(:ownerEmail)
              and (:cardId is null or card.id = :cardId)
              and (:cycleId is null or cycle.id = :cycleId)
            order by i.referenceYear desc, i.referenceMonth desc, card.name asc, i.id desc
            """)
    List<CreditCardInvoice> search(
            @Param("ownerEmail") String ownerEmail,
            @Param("cardId") Long cardId,
            @Param("cycleId") Long cycleId
    );
}
