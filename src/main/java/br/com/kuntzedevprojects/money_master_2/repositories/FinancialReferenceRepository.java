package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.FinancialReference;

public interface FinancialReferenceRepository extends JpaRepository<FinancialReference, Long> {

    @Query("""
            select r
            from FinancialReference r
            where (r.owner is null or lower(r.owner.email) = lower(:ownerEmail))
              and (:activeOnly = false or r.active = true)
            order by r.active desc, r.createdAt desc, r.id desc
            """)
    List<FinancialReference> findAvailableForOwner(@Param("ownerEmail") String ownerEmail, @Param("activeOnly") boolean activeOnly);

    @Query("""
            select r
            from FinancialReference r
            where r.id = :id
              and (r.owner is null or lower(r.owner.email) = lower(:ownerEmail))
            """)
    Optional<FinancialReference> findByIdAvailableForOwner(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);
}
