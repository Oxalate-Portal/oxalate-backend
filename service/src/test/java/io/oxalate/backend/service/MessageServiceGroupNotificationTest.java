package io.oxalate.backend.service;

import io.oxalate.backend.api.NotificationGroupEnum;
import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.model.Message;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MessageRepository;
import io.oxalate.backend.repository.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceGroupNotificationTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationGroupResolverService groupResolverService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MessageService messageService;

    private MessageRequest messageRequest;
    private Message mockMessage;
    private User mockUser;

    @BeforeEach
    void setUp() {
        messageRequest = new MessageRequest();
        messageRequest.setTitle("Test Title");
        messageRequest.setMessage("Test Message");
        messageRequest.setDescription("Test Description");
        messageRequest.setNotificationGroup(NotificationGroupEnum.ALL_REGISTERED);
        messageRequest.setCreator(1L);

        mockMessage = new Message();
        mockMessage.setId(100L);
        mockMessage.setTitle("Test Title");
        mockMessage.setMessage("Test Message");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        mockUser.setUsername("test@example.com");
    }

    @Test
    void testCreateNotificationForGroupWithValidGroup() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.ALL_REGISTERED);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.ALL_REGISTERED, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(3, result);
        verify(groupResolverService).resolveGroupUsers(NotificationGroupEnum.ALL_REGISTERED, null);
        verify(messageRepository).save(any(Message.class));
        verify(messageRepository, times(3)).addMessageReceiver(100L, anyLong());
        verify(emailService, times(3)).sendBulkNotificationEmail(any(User.class), anyString(), anyString(), any());
    }

    @Test
    void testCreateNotificationForGroupWithInactiveDays() {
        List<Long> userIds = Arrays.asList(1L, 2L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.INACTIVE_DAYS);
        messageRequest.setInactiveDays(7);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.INACTIVE_DAYS, 7))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(2, result);
        verify(groupResolverService).resolveGroupUsers(NotificationGroupEnum.INACTIVE_DAYS, 7);
        verify(messageRepository, times(2)).addMessageReceiver(100L, anyLong());
    }

    @Test
    void testCreateNotificationForGroupWithEmptyUserList() {
        messageRequest.setNotificationGroup(NotificationGroupEnum.LOCKED_ACCOUNTS);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.LOCKED_ACCOUNTS, null))
                .thenReturn(Collections.emptyList());

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(0, result);
        verify(groupResolverService).resolveGroupUsers(NotificationGroupEnum.LOCKED_ACCOUNTS, null);
        verify(messageRepository, never()).save(any(Message.class));
        verify(emailService, never()).sendBulkNotificationEmail(any(User.class), anyString(), anyString(), any());
    }

    @Test
    void testCreateNotificationForGroupWithActiveMembership() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.ACTIVE_MEMBERSHIP);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.ACTIVE_MEMBERSHIP, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(4, result);
        verify(messageRepository, times(4)).addMessageReceiver(100L, anyLong());
    }

    @Test
    void testCreateNotificationForGroupWithNoActiveMembership() {
        List<Long> userIds = Arrays.asList(5L, 6L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.NO_ACTIVE_MEMBERSHIP);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.NO_ACTIVE_MEMBERSHIP, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(2, result);
        verify(messageRepository, times(2)).addMessageReceiver(100L, anyLong());
    }

    @Test
    void testCreateNotificationForGroupWithNeverHadMembership() {
        List<Long> userIds = Arrays.asList(10L, 11L, 12L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.NEVER_HAD_MEMBERSHIP);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.NEVER_HAD_MEMBERSHIP, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(3, result);
        verify(messageRepository, times(3)).addMessageReceiver(100L, anyLong());
    }

    @Test
    void testCreateNotificationForGroupNullGroup() {
        messageRequest.setNotificationGroup(null);

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(0, result);
        verify(groupResolverService, never()).resolveGroupUsers(any(), any());
    }

    @Test
    void testCreateNotificationForGroupUserNotFound() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.ALL_REGISTERED);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.ALL_REGISTERED, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(userRepository.findById(3L)).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(3, result);
        // Even if user is not found, the notification should still be added to message_receiver table
        verify(messageRepository, times(3)).addMessageReceiver(100L, anyLong());
        // Email should only be sent for found users
        verify(emailService, times(2)).sendBulkNotificationEmail(any(User.class), anyString(), anyString(), any());
    }

    @Test
    void testCreateNotificationForGroupSavesMessageWithCorrectData() {
        List<Long> userIds = Arrays.asList(1L);
        messageRequest.setNotificationGroup(NotificationGroupEnum.ACTIVE_MEMBERSHIP);
        messageRequest.setTitle("Important Update");
        messageRequest.setMessage("Please read this");
        messageRequest.setDescription("Description");

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.ACTIVE_MEMBERSHIP, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        messageService.createNotificationForGroup(messageRequest, groupResolverService);

        verify(messageRepository).save(argThat(message ->
                "Important Update".equals(message.getTitle()) &&
                        "Please read this".equals(message.getMessage())
        ));
    }

    @Test
    void testCreateNotificationForGroupLargeUserList() {
        // Test with a large number of users
        List<Long> userIds = new java.util.ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            userIds.add(i);
        }

        messageRequest.setNotificationGroup(NotificationGroupEnum.ALL_REGISTERED);

        when(groupResolverService.resolveGroupUsers(NotificationGroupEnum.ALL_REGISTERED, null))
                .thenReturn(userIds);
        when(messageRepository.save(any(Message.class))).thenReturn(mockMessage);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        int result = messageService.createNotificationForGroup(messageRequest, groupResolverService);

        assertEquals(100, result);
        verify(messageRepository, times(100)).addMessageReceiver(100L, anyLong());
        verify(emailService, times(100)).sendBulkNotificationEmail(any(User.class), anyString(), anyString(), any());
    }
}
