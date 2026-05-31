package io.oxalate.backend.service;

import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.model.Message;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
// LENIENT needed: several tests stub userRepository.findById() with multiple different specific
// arguments in the same test method, which Mockito strict mode flags as PotentialStubbingProblem.
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceUTC {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MessageService messageService;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User buildUser(long id, String language) {
        return User.builder()
                   .id(id)
                   .username("user" + id + "@test.tld")
                   .firstName("Test")
                   .lastName("User")
                   .language(language)
                   .build();
    }

    private MessageRequest buildRequest(String title, String message, Long eventId) {
        return MessageRequest.builder()
                             .title(title)
                             .message(message)
                             .description("desc")
                             .creator(1L)
                             .eventId(eventId)
                             .build();
    }

    private Message buildSavedMessage(long id) {
        return Message.builder()
                      .id(id)
                      .title("title")
                      .message("msg")
                      .description("desc")
                      .creator(1L)
                      .createdAt(Instant.now())
                      .build();
    }

    // ------------------------------------------------------------------
    // createNotificationForUser
    // ------------------------------------------------------------------

    @Test
    void createNotificationForUserSendsEmailOk() {
        var user = buildUser(10L, "en");
        var request = buildRequest("Hello", "Body text", 42L);
        var savedMessage = buildSavedMessage(100L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        messageService.createNotificationForUser(request, 10L);

        verify(messageRepository).addMessageReceiver(100L, 10L);
        verify(emailService).sendBulkNotificationEmail(user, "Hello", "Body text", 42L);
    }

    @Test
    void createNotificationForUserSkipsEmailWhenUserNotFoundOk() {
        var request = buildRequest("Hello", "Body text", 42L);
        var savedMessage = buildSavedMessage(100L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        messageService.createNotificationForUser(request, 99L);

        verify(messageRepository).addMessageReceiver(100L, 99L);
        verify(emailService, never()).sendBulkNotificationEmail(any(), any(), any(), anyLong());
    }

    @Test
    void createNotificationForUserWithNullEventIdOk() {
        var user = buildUser(10L, "fi");
        var request = buildRequest("Hei", "Viesti", null);
        var savedMessage = buildSavedMessage(100L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        messageService.createNotificationForUser(request, 10L);

        verify(emailService).sendBulkNotificationEmail(user, "Hei", "Viesti", null);
    }

    // ------------------------------------------------------------------
    // createNotificationForUsers
    // ------------------------------------------------------------------

    @Test
    void createNotificationForUsersSendsEmailToEachRecipientOk() {
        var user1 = buildUser(1L, "en");
        var user2 = buildUser(2L, "fi");
        var request = buildRequest("Title", "Message", 7L);
        var savedMessage = buildSavedMessage(200L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        messageService.createNotificationForUsers(request, List.of(1L, 2L));

        verify(messageRepository).addMessageReceiver(200L, 1L);
        verify(messageRepository).addMessageReceiver(200L, 2L);
        verify(emailService).sendBulkNotificationEmail(user1, "Title", "Message", 7L);
        verify(emailService).sendBulkNotificationEmail(user2, "Title", "Message", 7L);
    }

    @Test
    void createNotificationForUsersSkipsMissingUsersEmailOk() {
        var user1 = buildUser(1L, "en");
        var request = buildRequest("Title", "Message", null);
        var savedMessage = buildSavedMessage(200L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        messageService.createNotificationForUsers(request, List.of(1L, 2L));

        verify(emailService, times(1)).sendBulkNotificationEmail(eq(user1), any(), any(), eq(null));
    }

    // ------------------------------------------------------------------
    // createNotificationForAllActiveUsers
    // ------------------------------------------------------------------

    @Test
    void createNotificationForAllActiveUsersSendsEmailToEachActiveUserOk() {
        var user1 = buildUser(1L, "en");
        var user2 = buildUser(2L, "de");
        var request = buildRequest("All Users", "Broadcast", 55L);
        var savedMessage = buildSavedMessage(300L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findAllActiveUsers()).thenReturn(List.of(user1, user2));

        var count = messageService.createNotificationForAllActiveUsers(request);

        assertEquals(2, count);
        verify(messageRepository).addMessageReceiver(300L, 1L);
        verify(messageRepository).addMessageReceiver(300L, 2L);
        verify(emailService).sendBulkNotificationEmail(user1, "All Users", "Broadcast", 55L);
        verify(emailService).sendBulkNotificationEmail(user2, "All Users", "Broadcast", 55L);
    }

    @Test
    void createNotificationForAllActiveUsersReturnsZeroWhenNoActiveUsersOk() {
        var request = buildRequest("Empty", "No users", null);
        var savedMessage = buildSavedMessage(300L);

        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(userRepository.findAllActiveUsers()).thenReturn(List.of());

        var count = messageService.createNotificationForAllActiveUsers(request);

        assertEquals(0, count);
        verify(emailService, never()).sendBulkNotificationEmail(any(), any(), any(), any());
    }
}
