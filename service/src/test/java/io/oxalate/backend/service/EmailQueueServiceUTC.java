package io.oxalate.backend.service;

import io.oxalate.backend.api.EmailNotificationDetailEnum;
import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EmailNotificationSubscriptionRepository;
import io.oxalate.backend.repository.EmailQueueRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.PageVersionRepository;
import io.oxalate.backend.repository.RoleRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
