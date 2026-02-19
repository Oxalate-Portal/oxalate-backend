package io.oxalate.backend.controller;

import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.MarkReadRequest;
import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.MessageResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_CREATE_BULK_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_CREATE_BULK_OK;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_CREATE_BULK_START;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_CREATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_CREATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_CREATE_START;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_GET_UNREAD_OK;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_GET_UNREAD_START;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_MARK_READ_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_MARK_READ_OK;
import static io.oxalate.backend.events.AppAuditMessages.NOTIFICATIONS_MARK_READ_START;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.rest.NotificationAPI;
import io.oxalate.backend.service.MessageService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("NotificationController")
public class NotificationController implements NotificationAPI {

    private final MessageService messageService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = NOTIFICATIONS_GET_UNREAD_START, okMessage = NOTIFICATIONS_GET_UNREAD_OK)
    public ResponseEntity<List<MessageResponse>> getUnreadNotifications() {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Getting unread notifications for user ID {}", userId);
        var notifications = messageService.getUnreadUserMessages(userId);
        return ResponseEntity.ok(notifications);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = NOTIFICATIONS_MARK_READ_START, okMessage = NOTIFICATIONS_MARK_READ_OK)
    public ResponseEntity<ActionResponse> markNotificationsAsRead(MarkReadRequest markReadRequest) {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Marking {} notifications as read for user ID {}", markReadRequest.getMessageIds()
                                                                                    .size(), userId);

        try {
            messageService.setMessagesAsRead(markReadRequest.getMessageIds(), userId);
            return ResponseEntity.ok(ActionResponse.builder()
                                                   .status(UpdateStatusEnum.OK)
                                                   .message("Notifications marked as read")
                                                   .build());
        } catch (Exception e) {
            log.error("Failed to mark notifications as read for user ID {}", userId, e);
            throw new OxalateValidationException(io.oxalate.backend.api.AuditLevelEnum.WARN, NOTIFICATIONS_MARK_READ_FAIL, HttpStatus.OK,
                    ActionResponse.builder()
                                  .status(UpdateStatusEnum.FAIL)
                                  .message("Failed to mark notifications as read: " + e.getMessage())
                                  .build());
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = NOTIFICATIONS_CREATE_START, okMessage = NOTIFICATIONS_CREATE_OK)
    public ResponseEntity<MessageResponse> createNotification(MessageRequest messageRequest) {
        var creatorId = AuthTools.getCurrentUserId();
        log.debug("Creating notification by admin user ID {}", creatorId);

        messageRequest.setCreator(creatorId);

        if (messageRequest.getRecipients() == null || messageRequest.getRecipients()
                                                                    .isEmpty()) {
            log.warn("No recipients specified for notification creation");
            throw new OxalateValidationException(io.oxalate.backend.api.AuditLevelEnum.WARN, NOTIFICATIONS_CREATE_FAIL, HttpStatus.BAD_REQUEST);
        }

        var recipientId = messageRequest.getRecipients()
                                        .getFirst();
        var messageResponse = messageService.createNotificationForUser(messageRequest, recipientId);
        return ResponseEntity.ok(messageResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = NOTIFICATIONS_CREATE_BULK_START, okMessage = NOTIFICATIONS_CREATE_BULK_OK)
    public ResponseEntity<ActionResponse> createBulkNotifications(MessageRequest messageRequest) {
        var creatorId = AuthTools.getCurrentUserId();
        log.debug("Creating bulk notifications by admin user ID {}", creatorId);

        messageRequest.setCreator(creatorId);

        try {
            int recipientCount;

            if (Boolean.TRUE.equals(messageRequest.getSendAll())) {
                recipientCount = messageService.createNotificationForAllActiveUsers(messageRequest);
                log.info("Created notification for all {} active users", recipientCount);
            } else if (messageRequest.getRecipients() != null && !messageRequest.getRecipients()
                                                                                .isEmpty()) {
                messageService.createNotificationForUsers(messageRequest, messageRequest.getRecipients());
                recipientCount = messageRequest.getRecipients()
                                               .size();
                log.info("Created notification for {} specified users", recipientCount);
            } else {
                log.warn("No recipients specified and sendAll is not set for bulk notification creation");
                throw new OxalateValidationException(io.oxalate.backend.api.AuditLevelEnum.WARN, NOTIFICATIONS_CREATE_BULK_FAIL, HttpStatus.OK,
                        ActionResponse.builder()
                                      .status(UpdateStatusEnum.FAIL)
                                      .message("No recipients specified and sendAll is not set")
                                      .build());
            }

            return ResponseEntity.ok(ActionResponse.builder()
                                                   .status(UpdateStatusEnum.OK)
                                                   .message("Notification sent to " + recipientCount + " users")
                                                   .build());
        } catch (OxalateValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create bulk notifications", e);
            throw new OxalateValidationException(io.oxalate.backend.api.AuditLevelEnum.WARN, NOTIFICATIONS_CREATE_BULK_FAIL, HttpStatus.OK,
                    ActionResponse.builder()
                                  .status(UpdateStatusEnum.FAIL)
                                  .message("Failed to create notifications: " + e.getMessage())
                                  .build());
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = NOTIFICATIONS_GET_ALL_START, okMessage = NOTIFICATIONS_GET_ALL_OK)
    public ResponseEntity<List<MessageResponse>> getAllNotifications() {
        var userId = AuthTools.getCurrentUserId();
        log.debug("Getting all notifications for user ID {}", userId);
        var notifications = messageService.getAllUserMessages(userId);
        return ResponseEntity.ok(notifications);
    }
}
