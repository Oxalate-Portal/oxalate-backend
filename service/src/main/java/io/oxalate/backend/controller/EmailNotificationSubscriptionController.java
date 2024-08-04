package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.INFO;
import io.oxalate.backend.api.request.EmailNotificationSubscriptionRequest;
import io.oxalate.backend.api.response.EmailNotificationSubscriptionResponse;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_SAVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_SAVE_START;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.EmailNotificationSubscriptionAPI;
import io.oxalate.backend.service.EmailNotificationSubscriptionService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
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
public class EmailNotificationSubscriptionController implements EmailNotificationSubscriptionAPI {
    private static final String AUDIT_NAME = "EmailNotificationSubscriptionController";

    private final EmailNotificationSubscriptionService emailNotificationSubscriptionService;
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EmailNotificationSubscriptionResponse>> getAllEmailNotificationSubscriptions(HttpServletRequest request) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(EMAIL_SUBSCRIPTION_GET_ALL_START, INFO, request, AUDIT_NAME, userId);

        var subscriptions = emailNotificationSubscriptionService.getAllForUser(userId);
        appEventPublisher.publishAuditEvent(EMAIL_SUBSCRIPTION_GET_ALL_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(subscriptions);
    }

    @Override
    public ResponseEntity<List<EmailNotificationSubscriptionResponse>> subscribeToEmailNotifications(HttpServletRequest request,
            EmailNotificationSubscriptionRequest subscriptions) {
        var userId = AuthTools.getCurrentUserId();
        var auditUuid = appEventPublisher.publishAuditEvent(EMAIL_SUBSCRIPTION_SAVE_START, INFO, request, AUDIT_NAME, userId);
        var subscriptionResponses = new ArrayList<EmailNotificationSubscriptionResponse>();
        emailNotificationSubscriptionService.removeAllSubscriptionsForUser(userId);

        for (var emailNotificationType : subscriptions.getSubscriptionList()) {
            var subscriptionResponse = emailNotificationSubscriptionService.subscribeToNotification(userId, emailNotificationType);
            subscriptionResponses.add(subscriptionResponse);
        }

        appEventPublisher.publishAuditEvent(EMAIL_SUBSCRIPTION_SAVE_OK, INFO, request, AUDIT_NAME, userId, auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(subscriptionResponses);
    }
}
