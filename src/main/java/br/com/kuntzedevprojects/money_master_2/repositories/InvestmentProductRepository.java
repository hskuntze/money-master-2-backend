package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;

public interface InvestmentProductRepository extends JpaRepository<InvestmentProduct, Long> {

    @Query("""
            select p
            from InvestmentProduct p
            left join fetch p.linkedAccount a
            where lower(p.owner.email) = lower(:ownerEmail)
            order by p.active desc, p.name asc
            """)
    List<InvestmentProduct> findByOwnerEmailWithAccount(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select p
            from InvestmentProduct p
            left join fetch p.linkedAccount a
            where p.id = :id
              and lower(p.owner.email) = lower(:ownerEmail)
            """)
    Optional<InvestmentProduct> findByIdAndOwnerEmailWithAccount(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    boolean existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(String ownerEmail, String name);

    Optional<InvestmentProduct> findByOwnerEmailIgnoreCaseAndNameIgnoreCase(String ownerEmail, String name);
}
