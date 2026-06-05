package br.com.kuntzedevprojects.money_master_2.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.Payment;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentSource;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            select p
            from Payment p
            join fetch p.owner own
            join fetch p.cycle cycle
            left join fetch p.payable payable
            left join fetch p.incomePlan incomePlan
            left join fetch p.transaction transaction
            left join fetch p.account account
            where p.id = :id
              and lower(own.email) = lower(:ownerEmail)
            """)
    Optional<Payment> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select p
            from Payment p
            join fetch p.owner own
            join fetch p.cycle cycle
            left join fetch p.payable payable
            left join fetch p.incomePlan incomePlan
            left join fetch p.transaction transaction
            left join fetch p.account account
            where lower(own.email) = lower(:ownerEmail)
              and (:cycleId is null or cycle.id = :cycleId)
              and (:from is null or p.paymentDate >= :from)
              and (:to is null or p.paymentDate <= :to)
            order by p.paymentDate desc, p.id desc
            """)
    List<Payment> search(
            @Param("ownerEmail") String ownerEmail,
            @Param("cycleId") Long cycleId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            select p
            from Payment p
            join fetch p.owner own
            join fetch p.cycle cycle
            left join fetch p.payable payable
            left join fetch p.incomePlan incomePlan
            left join fetch p.transaction transaction
            left join fetch p.account account
            where lower(own.email) = lower(:ownerEmail)
              and payable.id = :payableId
            order by p.paymentDate asc, p.id asc
            """)
    List<Payment> findByPayableId(@Param("ownerEmail") String ownerEmail, @Param("payableId") Long payableId);

    @Query("""
            select p
            from Payment p
            join fetch p.owner own
            join fetch p.cycle cycle
            left join fetch p.payable payable
            left join fetch p.incomePlan incomePlan
            left join fetch p.transaction transaction
            left join fetch p.account account
            where lower(own.email) = lower(:ownerEmail)
              and incomePlan.id = :incomePlanId
            order by p.paymentDate asc, p.id asc
            """)
    List<Payment> findByIncomePlanId(@Param("ownerEmail") String ownerEmail, @Param("incomePlanId") Long incomePlanId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.payable.id = :payableId
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus.ACTIVE
            """)
    BigDecimal sumActiveByPayable(@Param("payableId") Long payableId);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.incomePlan.id = :incomePlanId
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus.ACTIVE
            """)
    BigDecimal sumActiveByIncomePlan(@Param("incomePlanId") Long incomePlanId);

    @Query("""
            select max(p.paymentDate)
            from Payment p
            where (p.payable.id = :planItemId or p.incomePlan.id = :planItemId)
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus.ACTIVE
            """)
    LocalDate findLatestActivePaymentDateByPlanItem(@Param("planItemId") Long planItemId);

    @Query("""
            select p
            from Payment p
            join fetch p.owner owner
            left join fetch p.payable payable
            left join fetch p.transaction transaction
            where payable.id = :payableId
              and p.source = :source
              and p.status = br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus.ACTIVE
            order by p.id desc
            """)
    List<Payment> findActiveByPayableIdAndSource(@Param("payableId") Long payableId, @Param("source") PaymentSource source);
}
