package io.oxalate.backend.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class CertificateRepositoryFetchPlanUTC {

    @Test
    void certificateResponseQueriesFetchClassificationTranslations() throws NoSuchMethodException {
        var expected = new String[]{"classification", "classification.translations"};
        var methods = List.of(
                CertificateRepository.class.getMethod("findAll"),
                CertificateRepository.class.getMethod("findByUserIdOrderByCertificationDateAsc", long.class),
                CertificateRepository.class.getMethod("findByUserIdAndOrganizationAndAndCertificateName", long.class, String.class, String.class),
                CertificateRepository.class.getMethod("findByUserId", long.class));

        for (Method method : methods) {
            var entityGraph = method.getAnnotation(EntityGraph.class);
            assertNotNull(entityGraph, method.getName());
            assertArrayEquals(expected, entityGraph.attributePaths(), method.getName());
        }
    }
}
