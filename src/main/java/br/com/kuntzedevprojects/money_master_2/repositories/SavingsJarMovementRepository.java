package br.com.kuntzedevprojects.money_master_2.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.SavingsJarMovement;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType;

public interface SavingsJarMovementRepository extends JpaRepository<SavingsJarMovement, Long> {

    List<SavingsJarMovement> findBySavingsJarIdOrderByOccurredOnDescIdDesc(Long savingsJarId);

    @Query("""
            select coalesce(sum(
                case
                    when m.type = br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.WITHDRAWAL then -m.amount
                    else m.amount
                end
            ), 0)
            from SavingsJarMovement m
            where m.savingsJar.id = :savingsJarId
              and (:until is null or m.occurredOn <= :until)
            """)
    BigDecimal calculateCurrentAmountUntil(@Param("savingsJarId") Long savingsJarId, @Param("until") LocalDate until);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from SavingsJarMovement m
            where m.savingsJar.id = :savingsJarId
              and m.type in (
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.YIELD,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.INITIAL_YIELD
              )
              and (:until is null or m.occurredOn <= :until)
            """)
    BigDecimal calculateTotalYieldUntil(@Param("savingsJarId") Long savingsJarId, @Param("until") LocalDate until);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from SavingsJarMovement m
            where m.savingsJar.owner.email = :ownerEmail
              and m.type <> br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.WITHDRAWAL
            """)
    BigDecimal sumPositiveByOwner(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from SavingsJarMovement m
            where m.savingsJar.owner.email = :ownerEmail
              and m.type = br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.WITHDRAWAL
            """)
    BigDecimal sumWithdrawalsByOwner(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from SavingsJarMovement m
            where m.savingsJar.owner.email = :ownerEmail
              and m.type in (
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.YIELD,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.INITIAL_YIELD
              )
            """)
    BigDecimal sumYieldByOwner(@Param("ownerEmail") String ownerEmail);

    boolean existsBySavingsJarIdAndTypeAndOccurredOn(Long savingsJarId, SavingsJarMovementType type, LocalDate occurredOn);

    Optional<SavingsJarMovement> findTopBySavingsJarIdAndTypeOrderByOccurredOnDescIdDesc(Long savingsJarId, SavingsJarMovementType type);
}
