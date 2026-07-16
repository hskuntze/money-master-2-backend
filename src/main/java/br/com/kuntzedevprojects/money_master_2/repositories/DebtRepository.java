package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.Debt;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    @Query("""
            select distinct d
            from Debt d
            left join fetch d.account account
            left join fetch d.category category
            left join fetch d.installments installment
            left join fetch installment.financialPeriod cycle
            left join fetch installment.monthlyPlanItem planItem
            where lower(d.owner.email) = lower(:ownerEmail)
            order by d.status asc, d.firstDueDate asc, d.id asc
            """)
    List<Debt> findByOwnerEmailWithInstallments(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select distinct d
            from Debt d
            left join fetch d.account account
            left join fetch d.category category
            left join fetch d.installments installment
            left join fetch installment.financialPeriod cycle
            left join fetch installment.monthlyPlanItem planItem
            where d.id = :id
              and lower(d.owner.email) = lower(:ownerEmail)
            """)
    Optional<Debt> findByIdAndOwnerEmailWithInstallments(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);
}
