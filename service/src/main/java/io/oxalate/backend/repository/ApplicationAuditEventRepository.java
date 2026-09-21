package io.oxalate.backend.repository;

import io.oxalate.backend.model.ApplicationAuditEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationAuditEventRepository extends JpaRepository<ApplicationAuditEvent, Long>, JpaSpecificationExecutor<ApplicationAuditEvent> {
    @Modifying
    long deleteByCreatedAtBefore(Instant createdAt);
}
