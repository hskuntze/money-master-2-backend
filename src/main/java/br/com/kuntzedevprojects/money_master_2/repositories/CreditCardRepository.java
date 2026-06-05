package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.CreditCard;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    @Query("""
            select c
            from CreditCard c
            join fetch c.owner owner
            left join fetch c.account account
            where lower(owner.email) = lower(:ownerEmail)
            order by c.active desc, c.name asc, c.id asc
            """)
    List<CreditCard> findByOwnerEmail(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select c
            from CreditCard c
            join fetch c.owner owner
            left join fetch c.account account
            where c.id = :id
              and lower(owner.email) = lower(:ownerEmail)
            """)
    Optional<CreditCard> findByIdAndOwnerEmail(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);
}
