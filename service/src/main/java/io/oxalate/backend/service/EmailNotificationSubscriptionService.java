package io.oxalate.backend.service;

import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.api.response.EmailNotificationSubscriptionResponse;
import io.oxalate.backend.model.EmailNotificationSubscription;
import io.oxalate.backend.repository.EmailNotificationSubscriptionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailNotificationSubscriptionService {
    private final EmailNotificationSubscriptionRepository emailNotificationSubscriptionRepository;

    public List<EmailNotificationSubscriptionResponse> getAllForUser(long userId) {
        var subscriptions = emailNotificationSubscriptionRepository.findByUserIdOrderByIdAsc(userId);
        var subscriptionResponses = new ArrayList<EmailNotificationSubscriptionResponse>();

        for (var subscription : subscriptions) {
            subscriptionResponses.add(subscription.toResponse());
        }

        return subscriptionResponses;
    }

    public EmailNotificationSubscriptionResponse subscribeToNotification(long userId, EmailNotificationTypeEnum emailNotificationType) {
        // First check whether the subscription already exists
        var subscription = emailNotificationSubscriptionRepository.findByEmailNotificationTypeAndUserId(emailNotificationType, userId);

        if (subscription.isPresent()) {
            log.warn("Email notification for {} subscription for user {} already exists.", emailNotificationType, userId);
            return subscription.get().toResponse();
        }

        var emailNotificationSubscription = EmailNotificationSubscription.builder()
                                                                         .userId(userId)
                                                                         .emailNotificationType(emailNotificationType)
                                                                         .build();
        var newEmailNotificationSubscription = emailNotificationSubscriptionRepository.save(emailNotificationSubscription);

        return newEmailNotificationSubscription.toResponse();
    }

    public void removeAllSubscriptionsForUser(long userId) {
        var subscriptions = emailNotificationSubscriptionRepository.findByUserIdOrderByIdAsc(userId);

        for (var subscription : subscriptions) {
            emailNotificationSubscriptionRepository.deleteById(subscription.getId());
            log.info("Removed email notification subscription with id {} for user ID {}", subscription.getId(), userId);
        }
    }
}
