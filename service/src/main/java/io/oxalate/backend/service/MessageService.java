package io.oxalate.backend.service;

import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.api.response.MessageResponse;
import io.oxalate.backend.model.Message;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new message and returns the response without adding any receivers.
     * Use this method when you want to handle receivers separately.
     *
     * @param messageRequest The message request containing title, description, message, and creator
     * @return The created message response
     */
    @Transactional
    public MessageResponse save(MessageRequest messageRequest) {
        var message = Message.builder()
                .description(messageRequest.getDescription())
                .title(messageRequest.getTitle())
                .message(messageRequest.getMessage())
                .creator(messageRequest.getCreator())
                .createdAt(Instant.now())
                .build();

        message = messageRepository.save(message);
        return message.toMessageResponse();
    }

    /**
     * Creates a new notification for a single user.
     *
     * @param messageRequest The message request containing notification details
     * @param userId         The ID of the user to receive the notification
     * @return The created message response
     */
    @Transactional
    public MessageResponse createNotificationForUser(MessageRequest messageRequest, long userId) {
        var messageResponse = save(messageRequest);
        messageRepository.addMessageReceiver(messageResponse.getId(), userId);
        log.debug("Created notification with ID {} for user ID {}", messageResponse.getId(), userId);
        return messageResponse;
    }

    /**
     * Creates a new notification for a list of users.
     *
     * @param messageRequest The message request containing notification details
     * @param userIds        The list of user IDs to receive the notification
     * @return The created message response
     */
    @Transactional
    public MessageResponse createNotificationForUsers(MessageRequest messageRequest, List<Long> userIds) {
        var messageResponse = save(messageRequest);

        for (Long userId : userIds) {
            messageRepository.addMessageReceiver(messageResponse.getId(), userId);
        }

        log.debug("Created notification with ID {} for {} users", messageResponse.getId(), userIds.size());
        return messageResponse;
    }

    /**
     * Creates a new notification for all active (non-anonymized and non-locked) users.
     *
     * @param messageRequest The message request containing notification details
     * @return The number of users who received the notification
     */
    @Transactional
    public int createNotificationForAllActiveUsers(MessageRequest messageRequest) {
        var messageResponse = save(messageRequest);
        var activeUsers = userRepository.findAllActiveUsers();

        for (var user : activeUsers) {
            messageRepository.addMessageReceiver(messageResponse.getId(), user.getId());
        }

        log.debug("Created notification with ID {} for all {} active users", messageResponse.getId(), activeUsers.size());
        return activeUsers.size();
    }

    /**
     * Gets all unread notifications for a user.
     *
     * @param userId The ID of the user
     * @return List of unread message responses
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getUnreadUserMessages(long userId) {
        var unreadMessages = messageRepository.findUnreadUserMessages(userId);

        var messageResponses = new ArrayList<MessageResponse>();

        for (Message message : unreadMessages) {
            messageResponses.add(message.toMessageResponse());
        }

        return messageResponses;
    }

    /**
     * Gets all notifications for a user (both read and unread).
     *
     * @param userId The ID of the user
     * @return List of all message responses for the user
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getAllUserMessages(long userId) {
        var allMessages = messageRepository.findAllUserMessages(userId);

        var messageResponses = new ArrayList<MessageResponse>();

        for (Message message : allMessages) {
            messageResponses.add(message.toMessageResponse());
        }

        return messageResponses;
    }

    /**
     * Marks a single notification as read for a user.
     *
     * @param messageId The ID of the message to mark as read
     * @param userId The ID of the user
     */
    @Transactional
    public void setMessageAsRead(long messageId, long userId) {
        messageRepository.setMessageAsRead(messageId, userId);
        log.debug("Marked message ID {} as read for user ID {}", messageId, userId);
    }

    /**
     * Marks multiple notifications as read for a user.
     *
     * @param messageIds The list of message IDs to mark as read
     * @param userId     The ID of the user
     */
    @Transactional
    public void setMessagesAsRead(List<Long> messageIds, long userId) {
        if (messageIds == null || messageIds.isEmpty()) {
            log.debug("No message IDs provided to mark as read for user ID {}", userId);
            return;
        }

        messageRepository.setMessagesAsRead(messageIds, userId);
        log.debug("Marked {} messages as read for user ID {}", messageIds.size(), userId);
    }

    /**
     * Creates a simple notification for a user with just a title and message.
     * This is useful for system-generated notifications like email notifications.
     *
     * @param userId         The ID of the user to receive the notification
     * @param creatorId      The ID of the creator (can be system user ID)
     * @param title          The title of the notification
     * @param description    A short description of the notification
     * @param messageContent The content of the notification
     */
    @Transactional
    public void createSimpleNotification(long userId, long creatorId, String title, String description, String messageContent) {
        var message = Message.builder()
                             .description(description)
                             .title(title)
                             .message(messageContent)
                             .creator(creatorId)
                             .createdAt(Instant.now())
                             .build();

        message = messageRepository.save(message);
        messageRepository.addMessageReceiver(message.getId(), userId);
        log.debug("Created simple notification with ID {} for user ID {}", message.getId(), userId);
    }

    // Legacy method - keeping for backwards compatibility
    @Deprecated(since = "1.0", forRemoval = true)
    public List<MessageResponse> unreadUserMessages(long userId) {
        return getUnreadUserMessages(userId);
    }
}
