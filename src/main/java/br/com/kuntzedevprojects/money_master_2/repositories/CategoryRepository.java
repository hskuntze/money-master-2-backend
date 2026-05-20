package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.Category;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            select c
            from Category c
            left join c.owner o
            where c.active = true
              and (c.systemDefault = true or lower(o.email) = lower(:ownerEmail))
              and (:type is null or c.type = :type)
            order by c.systemDefault desc, c.name asc
            """)
    List<Category> findAvailableForUser(@Param("ownerEmail") String ownerEmail, @Param("type") TransactionType type);

    @Query("""
            select c
            from Category c
            left join c.owner o
            where c.id = :id
              and (c.systemDefault = true or lower(o.email) = lower(:ownerEmail))
            """)
    Optional<Category> findAvailableById(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select c
            from Category c
            where lower(c.name) = lower(:name)
              and c.type = :type
              and c.systemDefault = true
            """)
    Optional<Category> findSystemDefaultByNameAndType(@Param("name") String name, @Param("type") TransactionType type);

    @Query("""
            select c
            from Category c
            join c.owner o
            where lower(c.name) = lower(:name)
              and c.type = :type
              and lower(o.email) = lower(:ownerEmail)
            """)
    Optional<Category> findUserCategoryByNameAndType(
            @Param("ownerEmail") String ownerEmail,
            @Param("name") String name,
            @Param("type") TransactionType type
    );

    @Query("""
            select c
            from Category c
            left join c.owner o
            where lower(c.name) = lower(:name)
              and c.type = :type
              and (c.systemDefault = true or lower(o.email) = lower(:ownerEmail))
            order by c.systemDefault desc
            """)
    List<Category> findAvailableByNameAndType(
            @Param("ownerEmail") String ownerEmail,
            @Param("name") String name,
            @Param("type") TransactionType type
    );
}
