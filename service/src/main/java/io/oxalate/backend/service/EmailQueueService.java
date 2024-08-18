package io.oxalate.backend.service;

import io.oxalate.backend.api.EmailNotificationDetailEnum;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.api.EmailStatusEnum;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UserStatus;
import io.oxalate.backend.model.EmailQueueEntry;
import io.oxalate.backend.model.Role;
import io.oxalate.backend.repository.EmailNotificationSubscriptionRepository;
import io.oxalate.backend.repository.EmailQueueRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.PageVersionRepository;
import io.oxalate.backend.repository.RoleRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueService {

    @Value("${oxalate.mail.notification.retry-times}")
    private int retryLimit;

    @Value("${oxalate.language.default}")
    private String defaultLanguage;

    final private EmailQueueRepository emailQueueRepository;
    final private EmailService emailService;
    final private EmailNotificationSubscriptionRepository emailNotificationSubscriptionRepository;
    final private UserService userService;
    final private EventRepository eventRepository;
    final private PageVersionRepository pageVersionRepository;
    final private PageRoleAccessRepository pageRoleAccessRepository;
    final private RoleRepository roleRepository;

    @Transactional
    public void addNotification(EmailNotificationTypeEnum emailType, EmailNotificationDetailEnum detail, long typeId) {
        var subscriptions = emailNotificationSubscriptionRepository.findByEmailNotificationType(emailType);

        // First we flush out of the queue any notifications for the same type ID which have yet not been sent
        emailQueueRepository.deleteByTypeIdAndStatus(typeId, EmailStatusEnum.QUEUED.name());

        for (var subscription : subscriptions) {
            createEmailQueueEntry(subscription.getUserId(), emailType, detail, typeId);
        }
    }

    @Transactional
    public void createEmailQueueEntry(Long userId, EmailNotificationTypeEnum emailType, EmailNotificationDetailEnum detail, Long typeId) {
        // If the emailType is PAGE, then make sure the user can access the page
        if (emailType == EmailNotificationTypeEnum.PAGE) {
            var userRoles = roleRepository.findByUser(userId);
            var roleList = userRoles.stream()
                                    .map(Role::getName)
                                    .collect(Collectors.toSet());
            // All public pages are included
            roleList.add(RoleEnum.ROLE_ANONYMOUS);
            var roleAccess = pageRoleAccessRepository.findByPageIdAndRoleIn(typeId, roleList);

            if (roleAccess.isEmpty()) {
                log.debug("User with ID {} does not have access to page with ID {}", userId, typeId);
                return;
            }
        }

        var emailQueueEntry = EmailQueueEntry.builder()
                                             .userId(userId)
                                             .emailType(emailType)
                                             .emailDetail(detail)
                                             .typeId(typeId)
                                             .status(EmailStatusEnum.QUEUED)
                                             .counter(0)
                                             .nextSendTimestamp(Instant.now())
                                             .build();
        emailQueueRepository.save(emailQueueEntry);
    }

    @Transactional
    public void flushQueue() {
        var queuedEmails = emailQueueRepository.findUnprosessedNotifications();

        if (queuedEmails.isEmpty()) {
            log.info("No email notifications found to be send");
            return;
        }

        var counter = 0;

        for (var emailQueueEntry : queuedEmails) {
            if (emailQueueEntry.getCounter() >= retryLimit) {
                log.info("Email with ID {} for user ID {} has reached retry limit({}), removing from queue", emailQueueEntry.getId(),
                        emailQueueEntry.getUserId(), retryLimit);
                emailQueueRepository.delete(emailQueueEntry);
                continue;
            }

            try {
                emailQueueEntry.setStatus(EmailStatusEnum.SENDING);
                var sendingMessage = emailQueueRepository.save(emailQueueEntry);
                processQueueEntry(sendingMessage);
                sendingMessage.setStatus(EmailStatusEnum.SENT);
                emailQueueRepository.save(sendingMessage);
            } catch (Exception e) {
                log.error("Failed to send email with ID {}", emailQueueEntry.getId(), e);
                emailQueueEntry.setStatus(EmailStatusEnum.FAILED);
                emailQueueEntry.setCounter(emailQueueEntry.getCounter() + 1);
                // Retry period grows by 1 hour for each failed attempt until limit is reached
                emailQueueEntry.setNextSendTimestamp(Instant.now()
                                                            .plus(emailQueueEntry.getCounter(), ChronoUnit.HOURS));
                emailQueueRepository.save(emailQueueEntry);
            }

            counter++;
        }

        log.info("Processed {} email notifications", counter);
    }

    private void processQueueEntry(EmailQueueEntry sendingMessage) {
        var optionalUser = userService.findUserById(sendingMessage.getUserId());

        if (optionalUser.isEmpty()) {
            log.error("User with ID {} not found when attempting to send event email notification", sendingMessage.getUserId());
            return;
        }

        var user = optionalUser.get();

        // We only send notifications to active users
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            log.debug("User with ID {} is not active, skipping email notification", user.getId());
            return;
        }

        switch (sendingMessage.getEmailType()) {
        case EVENT:

            var optionalEvent = eventRepository.findById(sendingMessage.getTypeId());

            if (optionalEvent.isEmpty()) {
                log.error("Event with ID {} not found when attempting to send event email notification", sendingMessage.getTypeId());
                return;
            }

            var event = optionalEvent.get();

            emailService.sendEventNotificationEmail(user.getUsername(), user.getLanguage(), sendingMessage.getEmailDetail(), event);
            break;
        case PAGE:
            var optionalPageVersion = pageVersionRepository.findByPageIdAndLanguage(sendingMessage.getTypeId(), user.getLanguage());

            if (optionalPageVersion.isEmpty()) {
                log.warn("Could not find language {} of page version {} for user ID {}, fetching with default language {}",
                        user.getLanguage(), sendingMessage.getTypeId(), user.getId(), defaultLanguage);

                optionalPageVersion = pageVersionRepository.findByPageIdAndLanguage(sendingMessage.getTypeId(), defaultLanguage);

                if (optionalPageVersion.isEmpty()) {
                    log.error("Default page version with page ID {} not found when attempting to send page email notification", sendingMessage.getTypeId());
                    return;
                }
            }

            var pageVersion = optionalPageVersion.get();

            emailService.sendPageNotificationEmail(user.getUsername(), user.getLanguage(), sendingMessage.getEmailDetail(), pageVersion);
        default:
            log.error("Unknown email type {}", sendingMessage.getEmailType());
            break;
        }
    }
}
