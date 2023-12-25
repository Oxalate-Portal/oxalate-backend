package io.oxalate.backend.repository;

import io.oxalate.backend.model.Message;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CrudRepository<Message, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM messages m WHERE m.id IN (SELECT message_id FROM message_receivers WHERE user_id = :userId AND read = false)")
    List<Message> findUnreadUserMessages(@Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE message_receivers SET read = true WHERE message_id = :messageId AND user_id = :userId")
    void setMessageAsRead(@Param("messageId") long messageId, @Param("userId") long userId);
}