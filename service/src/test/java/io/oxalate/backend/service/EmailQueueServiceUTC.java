package io.oxalate.backend.service;

import io.oxalate.backend.api.EmailNotificationDetailEnum;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.api.EmailStatusEnum;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.model.EmailNotificationSubscription;
import io.oxalate.backend.model.EmailQueueEntry;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EmailNotificationSubscriptionRepository;
import io.oxalate.backend.repository.EmailQueueRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.PageVersionRepository;
import io.oxalate.backend.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailQueueServiceUTC {

    @Mock
    private EmailQueueRepository emailQueueRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailNotificationSubscriptionRepository emailNotificationSubscriptionRepository;

    @Mock
    private UserService userService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PageVersionRepository pageVersionRepository;

    @Mock
    private PageRoleAccessRepository pageRoleAccessRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PortalConfigurationService portalConfigurationService;

    @Mock
    private MessageService messageService;

    @Mock
    private NotificationLocalizationService notificationLocalizationService;

    @InjectMocks
    private EmailQueueService emailQueueService;

    @Test
    void createEmailQueueEntryCreatesGermanNotificationOk() {
        var user = User.builder()
                       .id(42L)
                       .language("de")
                       .build();
        when(userService.findUserEntityById(42L)).thenReturn(user);
        stubMessages("de");

        emailQueueService.createEmailQueueEntry(42L, EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.UPDATED, 7L);

        verifyLocalizedNotification("Neue E-Mail-Benachrichtigung", "E-Mail-Benachrichtigung",
                "Sie haben eine neue E-Mail-Benachrichtigung zu einer Veranstaltung: aktualisiert");
    }

    @Test
    void createEmailQueueEntryCreatesFinnishNotificationOk() {
        var user = User.builder()
                       .id(42L)
                       .language("fi")
                       .build();
        when(userService.findUserEntityById(42L)).thenReturn(user);
        stubMessages("fi");

        emailQueueService.createEmailQueueEntry(42L, EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.DELETED, 7L);

        verifyLocalizedNotification("Uusi sähköposti-ilmoitus", "Sähköposti-ilmoitus",
                "Sinulla on uusi sähköposti-ilmoitus tapahtumasta: poistettu");
    }

    @Test
    void addNotificationReplacesQueuedEntriesForSubscribers() {
        when(emailNotificationSubscriptionRepository.findByEmailNotificationType(EmailNotificationTypeEnum.EVENT))
                .thenReturn(List.of(EmailNotificationSubscription.builder()
                                                                 .userId(7L)
                                                                 .build()));

        emailQueueService.addNotification(EmailNotificationTypeEnum.EVENT, EmailNotificationDetailEnum.NEW, 9L);

        verify(emailQueueRepository).deleteByTypeIdAndStatus(9L, EmailStatusEnum.QUEUED.name());
        verify(emailQueueRepository).save(any(EmailQueueEntry.class));
    }

    @Test
    void pageQueueEntryIsSkippedWhenNoRoleAccessExists() {
        when(roleRepository.findByUser(7L)).thenReturn(java.util.Set.of());
        when(pageRoleAccessRepository.findByPageIdAndRoleIn(eq(9L), any())).thenReturn(java.util.Set.of());

        emailQueueService.createEmailQueueEntry(7L, EmailNotificationTypeEnum.PAGE,
                EmailNotificationDetailEnum.UPDATED, 9L);

        verify(emailQueueRepository, never()).save(any());
    }

    @Test
    void flushQueueDeletesEntryAtRetryLimit() {
        var entry = EmailQueueEntry.builder()
                                   .id(3L)
                                   .userId(7L)
                                   .counter(2)
                                   .build();
        when(emailQueueRepository.findUnprosessedNotifications()).thenReturn(List.of(entry));
        when(portalConfigurationService.getNumericConfiguration(any(), any())).thenReturn(2L);

        emailQueueService.flushQueue();

        verify(emailQueueRepository).delete(entry);
        assertEquals(2, entry.getCounter());
    }

    @Test
    void flushQueueMarksEntryFailedWhenProcessingThrows() {
        var entry = EmailQueueEntry.builder()
                                   .id(3L)
                                   .userId(7L)
                                   .counter(0)
                                   .emailType(EmailNotificationTypeEnum.EVENT)
                                   .emailDetail(EmailNotificationDetailEnum.NEW)
                                   .typeId(9L)
                                   .build();
        when(emailQueueRepository.findUnprosessedNotifications()).thenReturn(List.of(entry));
        when(portalConfigurationService.getNumericConfiguration(any(), any())).thenReturn(3L);
        when(emailQueueRepository.save(entry)).thenReturn(entry);
        when(userService.findUserEntityById(7L)).thenReturn(User.builder()
                                                                .id(7L)
                                                                .status(UserStatusEnum.ACTIVE)
                                                                .build());
        when(eventRepository.findById(9L)).thenThrow(new IllegalStateException("database unavailable"));

        emailQueueService.flushQueue();

        assertEquals(EmailStatusEnum.FAILED, entry.getStatus());
        assertEquals(1, entry.getCounter());
        verify(emailQueueRepository, org.mockito.Mockito.times(2)).save(entry);
    }

    @Test
    void flushQueueSendsEventAndMarksEntrySent() {
        var entry = EmailQueueEntry.builder()
                                   .id(3L)
                                   .userId(7L)
                                   .counter(0)
                                   .emailType(EmailNotificationTypeEnum.EVENT)
                                   .emailDetail(EmailNotificationDetailEnum.NEW)
                                   .typeId(9L)
                                   .build();
        var user = User.builder()
                       .id(7L)
                       .status(UserStatusEnum.ACTIVE)
                       .username("user@example.test")
                       .build();
        when(emailQueueRepository.findUnprosessedNotifications()).thenReturn(List.of(entry));
        when(portalConfigurationService.getNumericConfiguration(any(), any())).thenReturn(3L);
        when(emailQueueRepository.save(entry)).thenReturn(entry);
        when(userService.findUserEntityById(7L)).thenReturn(user);
        when(eventRepository.findById(9L)).thenReturn(Optional.of(Mockito.mock(io.oxalate.backend.model.Event.class)));

        emailQueueService.flushQueue();

        verify(emailService).sendEventNotificationEmail(any(), any(), any(), any());
        assertEquals(EmailStatusEnum.SENT, entry.getStatus());
    }

    private void stubMessages(String language) {
        when(notificationLocalizationService.getMessage(any(User.class), any())).thenAnswer(invocation -> {
            var key = invocation.getArgument(1, String.class);
            return switch (language + ":" + key) {
                case "de:notification.email.title" -> "Neue E-Mail-Benachrichtigung";
                case "de:notification.email.description" -> "E-Mail-Benachrichtigung";
                case "de:notification.email.detail.updated" -> "aktualisiert";
                case "fi:notification.email.title" -> "Uusi sähköposti-ilmoitus";
                case "fi:notification.email.description" -> "Sähköposti-ilmoitus";
                case "fi:notification.email.detail.deleted" -> "poistettu";
                default -> throw new AssertionError("Unexpected translation key: " + key);
            };
        });
        when(notificationLocalizationService.getMessage(any(User.class), any(), any())).thenAnswer(invocation -> {
            var key = invocation.getArgument(1, String.class);
            var args = invocation.getArgument(2, Object[].class);
            return switch (language + ":" + key) {
                case "de:notification.email.event" -> "Sie haben eine neue E-Mail-Benachrichtigung zu einer Veranstaltung: " + args[0];
                case "fi:notification.email.event" -> "Sinulla on uusi sähköposti-ilmoitus tapahtumasta: " + args[0];
                default -> throw new AssertionError("Unexpected translation key: " + key);
            };
        });
    }

    private void verifyLocalizedNotification(String expectedTitle, String expectedDescription, String expectedMessage) {
        var title = ArgumentCaptor.forClass(String.class);
        var description = ArgumentCaptor.forClass(String.class);
        var message = ArgumentCaptor.forClass(String.class);
        verify(messageService).createSimpleNotification(eq(42L), eq(1L), title.capture(), description.capture(), message.capture());
        assertEquals(expectedTitle, title.getValue());
        assertEquals(expectedDescription, description.getValue());
        assertEquals(expectedMessage, message.getValue());
        verify(emailQueueRepository).save(any());
    }
}
