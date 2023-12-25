package io.oxalate.backend.repository;

import io.oxalate.backend.model.ApplicationAuditEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationAuditEventRepository extends ListPagingAndSortingRepository<ApplicationAuditEvent, Long>,
        CrudRepository<ApplicationAuditEvent, Long> {
    @Modifying
    long deleteByCreatedAtBefore(Instant createdAt);

    Page<ApplicationAuditEvent> findByUserId(Long userId, PageRequest pageRequest);

    Page<ApplicationAuditEvent> findByTraceIdContainingIgnoreCase(String filter, PageRequest pageRequest);

    Page<ApplicationAuditEvent> findByMessageContainingIgnoreCase(String filter, PageRequest pageRequest);

    Page<ApplicationAuditEvent> findByIpAddressContainingIgnoreCase(String filter, PageRequest pageRequest);

    Page<ApplicationAuditEvent> findBySourceContainingIgnoreCase(String filter, PageRequest pageRequest);

    Page<ApplicationAuditEvent> findAllByUserIdIn(List<Long> userIds, PageRequest pageRequest);
}
