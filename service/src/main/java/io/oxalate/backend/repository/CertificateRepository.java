package io.oxalate.backend.repository;

import io.oxalate.backend.model.Certificate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateRepository extends CrudRepository<Certificate, Long> {

    Optional<Certificate> findByUserIdAndOrganizationAndAndCertificateName(long userId, String organization, String certificateName);
    List<Certificate> findByUserIdOrderByCertificationDateAsc(long userId);

    @EntityGraph(attributePaths = { "classification", "classification.translations" })
    List<Certificate> findByUserId(long userId);

    @Query("""
            select distinct c.certificateName
            from Certificate c
            where lower(c.certificateName) like lower(concat('%', :searchTerm, '%'))
            order by c.certificateName
            """)
    List<String> findDistinctCertificateNamesMatching(@Param("searchTerm") String searchTerm);

    @Query("""
            select distinct c.organization
            from Certificate c
            where lower(c.organization) like lower(concat('%', :searchTerm, '%'))
            order by c.organization
            """)
    List<String> findDistinctOrganizationsMatching(@Param("searchTerm") String searchTerm);

    @Modifying
    @Query(value = "UPDATE certificates SET classification_id = :classificationId WHERE id = :certificateId", nativeQuery = true)
    int updateClassification(@Param("certificateId") long certificateId, @Param("classificationId") Long classificationId);

    @Modifying
    @Query(value = "UPDATE certificates SET classification_id = :classificationId WHERE certificate_name IN (:certificateNames)", nativeQuery = true)
    int updateClassificationByNames(@Param("certificateNames") List<String> certificateNames, @Param("classificationId") Long classificationId);

    @Modifying
    @Query("update Certificate c set c.organization = :newValue where c.organization in :existingValues")
    int replaceOrganizations(@Param("existingValues") List<String> existingValues, @Param("newValue") String newValue);

    @Modifying
    @Query("update Certificate c set c.certificateName = :newValue where c.certificateName in :existingValues")
    int replaceCertificateNames(@Param("existingValues") List<String> existingValues, @Param("newValue") String newValue);
}
