package br.com.kuntzedevprojects.money_master_2.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.kuntzedevprojects.money_master_2.entities.SavingsJar;

public interface SavingsJarRepository extends JpaRepository<SavingsJar, Long> {

    @Query("""
            select j
            from SavingsJar j
            left join fetch j.linkedAccount a
            where lower(j.owner.email) = lower(:ownerEmail)
            order by j.active desc, j.name asc
            """)
    List<SavingsJar> findByOwnerEmailWithAccount(@Param("ownerEmail") String ownerEmail);

    @Query("""
            select j
            from SavingsJar j
            left join fetch j.linkedAccount a
            where j.id = :id
              and lower(j.owner.email) = lower(:ownerEmail)
            """)
    Optional<SavingsJar> findByIdAndOwnerEmailWithAccount(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    @Query("""
            select j
            from SavingsJar j
            where lower(j.owner.email) = lower(:ownerEmail)
              and lower(j.name) = lower(:name)
              and (
                    (:institutionName is null and j.institutionName is null)
                    or (:institutionName is not null and lower(j.institutionName) = lower(:institutionName))
              )
            """)
    Optional<SavingsJar> findByOwnerEmailAndNameAndInstitution(
            @Param("ownerEmail") String ownerEmail,
            @Param("name") String name,
            @Param("institutionName") String institutionName
    );

    @Query("""
            select j
            from SavingsJar j
            where j.active = true
              and j.yieldEnabled = true
              and j.yieldCalculationType = br.com.kuntzedevprojects.money_master_2.enums.SavingsJarYieldCalculationType.CDI_PERCENTAGE
            order by j.id asc
            """)
    List<SavingsJar> findActiveCdiYieldEnabled();
}
