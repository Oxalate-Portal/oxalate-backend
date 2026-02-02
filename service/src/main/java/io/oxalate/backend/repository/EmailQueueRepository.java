package io.oxalate.backend.repository;

import io.oxalate.backend.api.EmailStatusEnum;
import io.oxalate.backend.model.EmailQueueEntry;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailQueueRepository extends ListCrudRepository<EmailQueueEntry, Long> {
    List<EmailQueueEntry> findByStatus(EmailStatusEnum status);
    @Query("SELECT e FROM EmailQueueEntry e WHERE e.status = 'QUEUED' AND e.nextSendTimestamp <= CURRENT_TIMESTAMP")
    List<EmailQueueEntry> findUnprosessedNotifications();

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM email_queue WHERE type_id = :typeId AND status = :status")
	void deleteByTypeIdAndStatus(@Param("typeId") long typeId, @Param("status") String status);
}
