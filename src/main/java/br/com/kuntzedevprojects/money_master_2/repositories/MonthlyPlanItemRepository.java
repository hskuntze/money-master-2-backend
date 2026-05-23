package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;

public interface MonthlyPlanItemRepository extends JpaRepository<MonthlyPlanItem, Long> {

    @Query("""
            select i
            from MonthlyPlanItem i
            left join fetch i.account a
            left join fetch i.category c
            where lower(i.owner.email) = lower(:ownerEmail)
              and i.financialPeriod.id = :periodId
              and (:status is null or i.status = :status)
            order by i.dueDate asc, i.id asc
            """)
    List<MonthlyPlanItem> findByOwnerEmailAndPeriod(
            @Param("ownerEmail") String ownerEmail,
            @Param("periodId") Long periodId,
            @Param("status") MonthlyPlanItemStatus status
    );

    @Query("""
            select i
            from MonthlyPlanItem i
            left join fetch i.account a
            left join fetch i.category c
            join fetch i.financialPeriod p
            where lower(i.owner.email) = lower(:ownerEmail)
              and i.id = :id
            """)
    Optional<MonthlyPlanItem> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select i
            from MonthlyPlanItem i
            left join fetch i.account a
            left join fetch i.category c
            where lower(i.owner.email) = lower(:ownerEmail)
              and i.financialPeriod.id = :periodId
              and i.recurring = true
              and i.status <> br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus.CANCELED
            order by i.id asc
            """)
    List<MonthlyPlanItem> findRecurringByOwnerEmailAndPeriod(@Param("ownerEmail") String ownerEmail, @Param("periodId") Long periodId);

    @Query("""
            select i
            from MonthlyPlanItem i
            left join fetch i.account a
            left join fetch i.category c
            join fetch i.financialPeriod p
            where lower(i.owner.email) = lower(:ownerEmail)
              and i.financialPeriod.id = :periodId
              and (:type is null or i.type = :type)
              and i.status <> br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus.CANCELED
            order by i.dueDate asc, i.id asc
            """)
    List<MonthlyPlanItem> findActiveCandidatesByOwnerEmailAndPeriod(
            @Param("ownerEmail") String ownerEmail,
            @Param("periodId") Long periodId,
            @Param("type") br.com.kuntzedevprojects.money_master_2.enums.TransactionType type
    );

    @Query("""
            select i
            from MonthlyPlanItem i
            left join fetch i.account a
            left join fetch i.category c
            join fetch i.financialPeriod p
            where lower(i.owner.email) = lower(:ownerEmail)
              and i.financialPeriod.id = :periodId
              and (:type is null or i.type = :type)
              and i.status <> br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus.CANCELED
              and lower(i.description) like lower(concat('%', :description, '%'))
            order by i.dueDate asc, i.id asc
            """)
    List<MonthlyPlanItem> findByDescriptionContainingInPeriod(
            @Param("ownerEmail") String ownerEmail,
            @Param("periodId") Long periodId,
            @Param("type") br.com.kuntzedevprojects.money_master_2.enums.TransactionType type,
            @Param("description") String description
    );

}
