package io.oxalate.backend.service;

import io.oxalate.backend.service.filetransfer.CertificateFileTransferService;
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
    private final CertificateFileTransferService certificateFileTransferService;

    @Transactional
    public void anonymize(long userId) {
        eventService.anonymize(userId);
        userService.anonymize(userId);
        certificateFileTransferService.anonymize(userId);
        certificateService.anonymize(userId);
        emailNotificationSubscriptionService.removeAllSubscriptionsForUser(userId);
    }
}
