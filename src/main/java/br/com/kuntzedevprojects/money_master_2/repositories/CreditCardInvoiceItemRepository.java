package br.com.kuntzedevprojects.money_master_2.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.CreditCardInvoiceItem;

public interface CreditCardInvoiceItemRepository extends JpaRepository<CreditCardInvoiceItem, Long> {

    @Query("""
            select item
            from CreditCardInvoiceItem item
            join fetch item.owner owner
            join fetch item.invoice invoice
            left join fetch item.category category
            where lower(owner.email) = lower(:ownerEmail)
              and invoice.id = :invoiceId
            order by item.purchaseDate asc, item.id asc
            """)
    List<CreditCardInvoiceItem> findByInvoiceIdAndOwnerEmail(
            @Param("ownerEmail") String ownerEmail,
            @Param("invoiceId") Long invoiceId
    );

    @Query("""
            select item
            from CreditCardInvoiceItem item
            join fetch item.owner owner
            join fetch item.invoice invoice
            left join fetch item.category category
            where item.id = :id
              and lower(owner.email) = lower(:ownerEmail)
            """)
    Optional<CreditCardInvoiceItem> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select coalesce(sum(item.amount), 0)
            from CreditCardInvoiceItem item
            where item.invoice.id = :invoiceId
            """)
    BigDecimal sumByInvoiceId(@Param("invoiceId") Long invoiceId);
}
