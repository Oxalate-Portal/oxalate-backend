package io.oxalate.backend.repository;

import io.oxalate.backend.model.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query(nativeQuery = true, value = """
            SELECT m.id, m.description, m.title, m.message, m.creator, m.created_at AS createdAt, mr.read
            FROM messages m
            JOIN message_receivers mr ON m.id = mr.message_id
            WHERE mr.user_id = :userId AND mr.read = false
            """)
    List<MessageWithReadStatus> findUnreadUserMessages(@Param("userId") long userId);

    @Query(nativeQuery = true, value = """
            SELECT m.id, m.description, m.title, m.message, m.creator, m.created_at AS createdAt, mr.read
            FROM messages m
            JOIN message_receivers mr ON m.id = mr.message_id
            WHERE mr.user_id = :userId ORDER BY m.created_at DESC
            """)
    List<MessageWithReadStatus> findAllUserMessages(@Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE message_receivers SET read = true
            WHERE message_id = :messageId AND user_id = :userId
            """)
    void setMessageAsRead(@Param("messageId") long messageId, @Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE message_receivers SET read = true
            WHERE message_id IN (:messageIds) AND user_id = :userId
            """)
    void setMessagesAsRead(@Param("messageIds") List<Long> messageIds, @Param("userId") long userId);

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO message_receivers (message_id, user_id, read)
            VALUES (:messageId, :userId, false)
            """)
    void addMessageReceiver(@Param("messageId") long messageId, @Param("userId") long userId);
}
