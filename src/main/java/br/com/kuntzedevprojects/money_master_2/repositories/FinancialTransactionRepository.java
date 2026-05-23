package br.com.kuntzedevprojects.money_master_2.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    @Query("""
            select t
            from FinancialTransaction t
            join fetch t.account a
            left join fetch t.category c
            where lower(t.owner.email) = lower(:ownerEmail)
              and (:from is null or t.occurredOn >= :from)
              and (:to is null or t.occurredOn <= :to)
              and (:accountId is null or a.id = :accountId)
              and (:categoryId is null or c.id = :categoryId)
              and (:type is null or t.type = :type)
            order by t.occurredOn desc, t.id desc
            """)
    List<FinancialTransaction> search(
            @Param("ownerEmail") String ownerEmail,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type
    );

    @Query("""
            select t
            from FinancialTransaction t
            join fetch t.account a
            left join fetch t.category c
            where t.id = :id
              and lower(t.owner.email) = lower(:ownerEmail)
            """)
    Optional<FinancialTransaction> findByIdAndOwnerEmailWithDetails(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from FinancialTransaction t
            where lower(t.owner.email) = lower(:ownerEmail)
              and (:accountId is null or t.account.id = :accountId)
              and (:from is null or t.occurredOn >= :from)
              and (:to is null or t.occurredOn <= :to)
              and t.type = :type
            """)
    BigDecimal sumAmount(
            @Param("ownerEmail") String ownerEmail,
            @Param("accountId") Long accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") TransactionType type
    );

    @Query("""
            select t
            from FinancialTransaction t
            join fetch t.account a
            left join fetch t.category c
            where lower(t.owner.email) = lower(:ownerEmail)
              and c is not null
              and lower(c.name) = lower(:categoryName)
              and (:type is null or t.type = :type)
              and (:from is null or t.occurredOn >= :from)
              and (:to is null or t.occurredOn <= :to)
            order by t.occurredOn desc, t.id desc
            """)
    List<FinancialTransaction> findByOwnerCategoryNameAndTypeAndPeriod(
            @Param("ownerEmail") String ownerEmail,
            @Param("categoryName") String categoryName,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            select t
            from FinancialTransaction t
            join fetch t.account a
            left join fetch t.category c
            where lower(t.owner.email) = lower(:ownerEmail)
              and t.occurredOn = :occurredOn
              and (:type is null or t.type = :type)
              and lower(t.description) like lower(concat('%', :description, '%'))
            order by t.id desc
            """)
    List<FinancialTransaction> findByOwnerDescriptionAndDate(
            @Param("ownerEmail") String ownerEmail,
            @Param("description") String description,
            @Param("occurredOn") LocalDate occurredOn,
            @Param("type") TransactionType type
    );

}
