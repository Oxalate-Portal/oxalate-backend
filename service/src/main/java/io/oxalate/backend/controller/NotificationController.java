package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevelEnum.INFO;
import static io.oxalate.backend.api.AuditLevelEnum.WARN;
import io.oxalate.backend.api.UpdateStatusEnum;
import io.oxalate.backend.api.request.MarkReadRequest;
import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.api.response.ActionResponse;
import io.oxalate.backend.api.response.MessageResponse;
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
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.NotificationAPI;
import io.oxalate.backend.service.MessageService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class NotificationController implements NotificationAPI {

    private static final String AUDIT_NAME = "NotificationController";
    private final MessageService messageService;
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MessageResponse>> getUnreadNotifications(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(NOTIFICATIONS_GET_UNREAD_START, INFO, request, AUDIT_NAME, userId);
        log.debug("Getting unread notifications for user ID {}", userId);
        var notifications = messageService.getUnreadUserMessages(userId);
        appEventPublisher.publishAuditEvent(NOTIFICATIONS_GET_UNREAD_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(notifications);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<ActionResponse> markNotificationsAsRead(MarkReadRequest markReadRequest, HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(NOTIFICATIONS_MARK_READ_START, INFO, request, AUDIT_NAME, userId);
        log.debug("Marking {} notifications as read for user ID {}", markReadRequest.getMessageIds()
                                                                                    .size(), userId);

        try {
            messageService.setMessagesAsRead(markReadRequest.getMessageIds(), userId);
            appEventPublisher.publishAuditEvent(NOTIFICATIONS_MARK_READ_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.ok(ActionResponse.builder()
                                                   .status(UpdateStatusEnum.OK)
                                                   .message("Notifications marked as read")
                                                   .build());
        } catch (Exception e) {
            log.error("Failed to mark notifications as read for user ID {}", userId, e);
            appEventPublisher.publishAuditEvent(NOTIFICATIONS_MARK_READ_FAIL, WARN, request, AUDIT_NAME, userId, auditUuid);
            return ResponseEntity.ok(ActionResponse.builder()
                                                   .status(UpdateStatusEnum.FAIL)
                                                   .message("Failed to mark notifications as read: " + e.getMessage())
                                                   .build());
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> createNotification(MessageRequest messageRequest, HttpServletRequest request) {
        var creatorId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_START, INFO, request, AUDIT_NAME, creatorId);
        log.debug("Creating notification by admin user ID {}", creatorId);

        // Set the creator to the current user
        messageRequest.setCreator(creatorId);

        if (messageRequest.getRecipients() == null || messageRequest.getRecipients()
                                                                    .isEmpty()) {
            log.warn("No recipients specified for notification creation");
            appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_FAIL, WARN, request, AUDIT_NAME, creatorId, auditUuid);
            return ResponseEntity.badRequest()
                                 .build();
        }

        // Create notification for the first recipient (single user creation endpoint)
        var recipientId = messageRequest.getRecipients()
                                        .getFirst();
        var messageResponse = messageService.createNotificationForUser(messageRequest, recipientId);
        appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_OK, INFO, request, AUDIT_NAME, creatorId, auditUuid);
        return ResponseEntity.ok(messageResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ActionResponse> createBulkNotifications(MessageRequest messageRequest, HttpServletRequest request) {
        var creatorId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_BULK_START, INFO, request, AUDIT_NAME, creatorId);
        log.debug("Creating bulk notifications by admin user ID {}", creatorId);

        // Set the creator to the current user
        messageRequest.setCreator(creatorId);

        try {
            int recipientCount;

            if (Boolean.TRUE.equals(messageRequest.getSendAll())) {
                // Send to all active users
                recipientCount = messageService.createNotificationForAllActiveUsers(messageRequest);
                log.info("Created notification for all {} active users", recipientCount);
            } else if (messageRequest.getRecipients() != null && !messageRequest.getRecipients()
                                                                                .isEmpty()) {
                // Send to specific list of users
                messageService.createNotificationForUsers(messageRequest, messageRequest.getRecipients());
                recipientCount = messageRequest.getRecipients()
                                               .size();
                log.info("Created notification for {} specified users", recipientCount);
            } else {
                log.warn("No recipients specified and sendAll is not set for bulk notification creation");
                appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_BULK_FAIL, WARN, request, AUDIT_NAME, creatorId, auditUuid);
                return ResponseEntity.ok(ActionResponse.builder()
                                                       .status(UpdateStatusEnum.FAIL)
                                                       .message("No recipients specified and sendAll is not set")
                                                       .build());
            }

            appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_BULK_OK, INFO, request, AUDIT_NAME, creatorId, auditUuid);
            return ResponseEntity.ok(ActionResponse.builder()
                                                   .status(UpdateStatusEnum.OK)
                                                   .message("Notification sent to " + recipientCount + " users")
                                                   .build());
        } catch (Exception e) {
            log.error("Failed to create bulk notifications", e);
            appEventPublisher.publishAuditEvent(NOTIFICATIONS_CREATE_BULK_FAIL, WARN, request, AUDIT_NAME, creatorId, auditUuid);
            return ResponseEntity.ok(ActionResponse.builder()
                                                   .status(UpdateStatusEnum.FAIL)
                                                   .message("Failed to create notifications: " + e.getMessage())
                                                   .build());
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<MessageResponse>> getAllNotifications(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(NOTIFICATIONS_GET_ALL_START, INFO, request, AUDIT_NAME, userId);
        log.debug("Getting all notifications for user ID {}", userId);
        var notifications = messageService.getAllUserMessages(userId);
        appEventPublisher.publishAuditEvent(NOTIFICATIONS_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.ok(notifications);
    }
}
