package br.com.kuntzedevprojects.money_master_2.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;

public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, Long> {

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
            order by p.startDate desc, p.id desc
            """)
    List<FinancialPeriod> findByOwnerEmailOrderByStartDateDesc(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.status = :status
            order by p.startDate desc, p.id desc
            """)
    List<FinancialPeriod> findByOwnerEmailAndStatus(@Param("ownerEmail") String ownerEmail, @Param("status") FinancialPeriodStatus status);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.id = :id
            """)
    Optional<FinancialPeriod> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.startDate = :startDate
            order by p.id desc
            """)
    Optional<FinancialPeriod> findByOwnerEmailAndStartDate(@Param("ownerEmail") String ownerEmail, @Param("startDate") LocalDate startDate);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.startDate <= :date
              and p.endDate >= :date
            order by p.startDate desc, p.id desc
            """)
    Optional<FinancialPeriod> findPeriodContainingDate(@Param("ownerEmail") String ownerEmail, @Param("date") LocalDate date);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.endDate < :date
            order by p.endDate desc, p.id desc
            """)
    List<FinancialPeriod> findLatestBeforeDate(@Param("ownerEmail") String ownerEmail, @Param("date") LocalDate date);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus.OPEN
            order by p.startDate desc, p.id desc
            """)
    List<FinancialPeriod> findOpenPeriods(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select p
            from FinancialPeriod p
            where lower(p.owner.email) = lower(:ownerEmail)
              and p.id <> :periodId
              and p.startDate <= :endDate
              and p.endDate >= :startDate
            order by p.startDate asc, p.id asc
            """)
    List<FinancialPeriod> findOverlappingPeriods(
            @Param("ownerEmail") String ownerEmail,
            @Param("periodId") Long periodId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
