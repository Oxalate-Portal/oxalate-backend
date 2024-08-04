package io.oxalate.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnonymizeService {
    private final UserService userService;
    private final CertificateService certificateService;
    private final EventService eventService;
    private final EmailNotificationSubscriptionService emailNotificationSubscriptionService;

    @Transactional
    public void anonymize(long userId) {
        eventService.anonymize(userId);
        userService.anonymize(userId);
        certificateService.anonymize(userId);
        emailNotificationSubscriptionService.removeAllSubscriptionsForUser(userId);
    }
}
