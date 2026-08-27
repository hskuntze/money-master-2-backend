package br.com.kuntzedevprojects.money_master_2.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.InvestmentMovement;

public interface InvestmentMovementRepository extends JpaRepository<InvestmentMovement, Long> {

    List<InvestmentMovement> findByInvestmentProductIdOrderByOccurredOnDescIdDesc(Long investmentProductId);

    boolean existsByInvestmentProductId(Long investmentProductId);

    @Query("""
            select coalesce(sum(
                case
                    when m.type = br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType.WITHDRAWAL then -m.amount
                    else m.amount
                end
            ), 0)
            from InvestmentMovement m
            where m.investmentProduct.id = :productId
              and (:until is null or m.occurredOn <= :until)
            """)
    BigDecimal calculateCurrentAmountUntil(@Param("productId") Long productId, @Param("until") LocalDate until);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from InvestmentMovement m
            where m.investmentProduct.id = :productId
              and m.type in (
                    br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType.INITIAL_BALANCE,
                    br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType.CONTRIBUTION
              )
            """)
    BigDecimal calculateTotalContributed(@Param("productId") Long productId);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from InvestmentMovement m
            where m.investmentProduct.id = :productId
              and m.type = br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType.WITHDRAWAL
            """)
    BigDecimal calculateTotalWithdrawn(@Param("productId") Long productId);

    @Query("""
            select coalesce(sum(m.amount), 0)
            from InvestmentMovement m
            where m.investmentProduct.id = :productId
              and m.type in (
                    br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType.YIELD,
                    br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType.YIELD_ADJUSTMENT
              )
            """)
    BigDecimal calculateTotalYield(@Param("productId") Long productId);
}
