package io.oxalate.backend.controller;

import io.oxalate.backend.api.request.EmailNotificationSubscriptionRequest;
import io.oxalate.backend.api.response.EmailNotificationSubscriptionResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_GET_ALL_OK;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_GET_ALL_START;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_SAVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.EMAIL_SUBSCRIPTION_SAVE_START;
import io.oxalate.backend.rest.EmailNotificationSubscriptionAPI;
import io.oxalate.backend.service.EmailNotificationSubscriptionService;
import io.oxalate.backend.tools.AuthTools;
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
@AuditSource("EmailNotificationSubscriptionController")
public class EmailNotificationSubscriptionController implements EmailNotificationSubscriptionAPI {

    private final EmailNotificationSubscriptionService emailNotificationSubscriptionService;

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = EMAIL_SUBSCRIPTION_GET_ALL_START, okMessage = EMAIL_SUBSCRIPTION_GET_ALL_OK)
    public ResponseEntity<List<EmailNotificationSubscriptionResponse>> getAllEmailNotificationSubscriptions() {
        var userId = AuthTools.getCurrentUserId();
        var subscriptions = emailNotificationSubscriptionService.getAllForUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(subscriptions);
    }

    @Override
    @Audited(startMessage = EMAIL_SUBSCRIPTION_SAVE_START, okMessage = EMAIL_SUBSCRIPTION_SAVE_OK)
    public ResponseEntity<List<EmailNotificationSubscriptionResponse>> subscribeToEmailNotifications(
            EmailNotificationSubscriptionRequest subscriptions) {
        var userId = AuthTools.getCurrentUserId();
        var subscriptionResponses = new ArrayList<EmailNotificationSubscriptionResponse>();
        emailNotificationSubscriptionService.removeAllSubscriptionsForUser(userId);

        for (var emailNotificationType : subscriptions.getSubscriptionList()) {
            var subscriptionResponse = emailNotificationSubscriptionService.subscribeToNotification(userId, emailNotificationType);
            subscriptionResponses.add(subscriptionResponse);
        }

        return ResponseEntity.status(HttpStatus.OK).body(subscriptionResponses);
    }
}
