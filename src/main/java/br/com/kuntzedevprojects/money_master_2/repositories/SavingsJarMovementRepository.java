package br.com.kuntzedevprojects.money_master_2.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.SavingsJarMovement;
import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType;

public interface SavingsJarMovementRepository extends JpaRepository<SavingsJarMovement, Long> {

    List<SavingsJarMovement> findBySavingsJarIdOrderByOccurredOnDescIdDesc(Long savingsJarId);

    @Modifying
    @Query("""
            delete from SavingsJarMovement m
            where m.savingsJar.id = :savingsJarId
            """)
    int deleteBySavingsJarId(@Param("savingsJarId") Long savingsJarId);

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
            select coalesce(sum(
                case
                    when m.type in (
                        br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.WITHDRAWAL,
                        br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.TRANSFER_OUT
                    ) then -m.amount
                    else m.amount
                end
            ), 0)
            from SavingsJarMovement m
            where lower(m.savingsJar.owner.email) = lower(:ownerEmail)
              and m.savingsJar.linkedAccount.id = :accountId
            """)
    BigDecimal calculateReservedAmountByLinkedAccount(
            @Param("ownerEmail") String ownerEmail,
            @Param("accountId") Long accountId
    );

    @Query("""
            select coalesce(sum(m.amount), 0)
            from SavingsJarMovement m
            where m.savingsJar.id = :savingsJarId
              and m.type in (
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.YIELD,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.INITIAL_YIELD,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.YIELD_ADJUSTMENT
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
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.INITIAL_YIELD,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.YIELD_ADJUSTMENT
              )
            """)
    BigDecimal sumYieldByOwner(@Param("ownerEmail") String ownerEmail);

    boolean existsBySavingsJarIdAndTypeAndOccurredOn(Long savingsJarId, SavingsJarMovementType type, LocalDate occurredOn);

    boolean existsBySavingsJarIdAndReferenceKey(Long savingsJarId, String referenceKey);

    boolean existsBySavingsJarId(Long savingsJarId);

    Optional<SavingsJarMovement> findTopBySavingsJarIdAndTypeOrderByOccurredOnDescIdDesc(Long savingsJarId, SavingsJarMovementType type);

    @Query("""
            select coalesce(sum(
                case
                    when m.type = br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.WITHDRAWAL then -m.amount
                    else m.amount
                end
            ), 0)
            from SavingsJarMovement m
            where m.savingsJar.id = :savingsJarId
              and (:fromExclusive is null or m.occurredOn > :fromExclusive)
              and (:toInclusive is null or m.occurredOn <= :toInclusive)
              and m.type in (
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.INITIAL_BALANCE,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.DEPOSIT,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.WITHDRAWAL,
                    br.com.kuntzedevprojects.money_master_2.enums.SavingsJarMovementType.ADJUSTMENT
              )
            """)
    BigDecimal sumPrincipalEffectBetween(
            @Param("savingsJarId") Long savingsJarId,
            @Param("fromExclusive") LocalDate fromExclusive,
            @Param("toInclusive") LocalDate toInclusive
    );

    @Query("""
            select max(m.occurredOn)
            from SavingsJarMovement m
            where m.savingsJar.id = :savingsJarId
              and m.type in :types
            """)
    Optional<LocalDate> findLatestMovementDateBySavingsJarIdAndTypes(
            @Param("savingsJarId") Long savingsJarId,
            @Param("types") Set<SavingsJarMovementType> types
    );
}
