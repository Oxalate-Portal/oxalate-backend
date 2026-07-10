package io.oxalate.backend.service.commenting;

import io.oxalate.backend.api.CommentStatusEnum;
import io.oxalate.backend.api.CommentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.COMMENTING;
import static io.oxalate.backend.api.PortalConfigEnum.CommentConfigEnum.COMMENT_REQUIRE_REVIEW;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.commenting.Comment;
import io.oxalate.backend.model.commenting.EventComment;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.commenting.CommentReportRepository;
import io.oxalate.backend.repository.commenting.CommentRepository;
import io.oxalate.backend.repository.commenting.EventCommentRepository;
import io.oxalate.backend.repository.filetransfer.AvatarFileRepository;
import io.oxalate.backend.service.MessageService;
import io.oxalate.backend.service.PortalConfigurationService;
import io.oxalate.backend.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceUTC {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserService userService;

    @Mock
    private EventCommentRepository eventCommentRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private AvatarFileRepository avatarFileRepository;

    @Mock
    private PortalConfigurationService portalConfigurationService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CommentService commentService;

    private User buildUser(long id, String language, String firstName, String lastName) {
        return User.builder()
                   .id(id)
                   .username("user" + id + "@test.tld")
                   .firstName(firstName)
                   .lastName(lastName)
                   .language(language)
                   .registered(Instant.now()
                                      .minusSeconds(86_400))
                   .build();
    }

    private Comment buildComment(long id, long userId, Long parentCommentId, String title, String body) {
        return Comment.builder()
                      .id(id)
                      .userId(userId)
                      .parentCommentId(parentCommentId)
                      .title(title)
                      .body(body)
                      .commentType(CommentTypeEnum.USER_COMMENT)
                      .commentStatus(CommentStatusEnum.PUBLISHED)
                      .createdAt(Instant.now())
                      .build();
    }

    @Test
    void createCommentOnDiveEventSendsLocalizedNotificationsOk() {
        var author = buildUser(1L, "en", "Alice", "Anderson");
        var participant = buildUser(2L, "de", "Bertil", "Berg");
        var organizer = buildUser(3L, "es", "Carlos", "Cruz");
        var event = Event.builder()
                         .id(55L)
                         .title("Dive event 55")
                         .organizerId(3L)
                         .build();
        var parentEventTopicComment = buildComment(900L, 3L, 1L, "Root topic comment for event ID: 55", "Root topic comment for event ID: 55");
        var savedComment = buildComment(999L, 1L, 900L, "Great dive", "Great dive body");
        var request = CommentRequest.builder()
                                    .title("Great dive")
                                    .body("Great dive body")
                                    .parentCommentId(900L)
                                    .commentType(CommentTypeEnum.USER_COMMENT)
                                    .build();

        when(userService.findUserEntityById(1L)).thenReturn(author);
        when(userService.findUserEntityById(3L)).thenReturn(organizer);
        when(userService.findEventParticipants(55L)).thenReturn(List.of(author, participant));
        when(commentRepository.findById(900L)).thenReturn(Optional.of(parentEventTopicComment));
        when(commentRepository.findById(999L)).thenReturn(Optional.of(savedComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentRepository.countChildren(999L)).thenReturn(0L);
        when(eventCommentRepository.findByComment_Id(999L)).thenReturn(Optional.empty());
        when(eventCommentRepository.findByComment_Id(900L)).thenReturn(Optional.of(EventComment.builder()
                                                                                               .eventId(55L)
                                                                                               .comment(parentEventTopicComment)
                                                                                               .build()));
        when(eventRepository.findById(55L)).thenReturn(Optional.of(event));
        when(avatarFileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(portalConfigurationService.getBooleanConfiguration(COMMENTING.group, COMMENT_REQUIRE_REVIEW.key)).thenReturn(false);

        when(messageSource.getMessage(eq("notification.event-comment.title"), any(Object[].class), any(Locale.class))).thenAnswer(invocation -> {
            var args = invocation.getArgument(1, Object[].class);
            var locale = invocation.getArgument(2, Locale.class);
            return switch (locale.getLanguage()) {
                case "de" -> "Neuer Kommentar zu " + args[0];
                case "es" -> "Nuevo comentario en " + args[0];
                default -> "New comment on " + args[0];
            };
        });
        when(messageSource.getMessage(eq("notification.event-comment.message"), any(Object[].class), any(Locale.class))).thenAnswer(invocation -> {
            var args = invocation.getArgument(1, Object[].class);
            var locale = invocation.getArgument(2, Locale.class);
            return switch (locale.getLanguage()) {
                case "de" -> args[0] + " hat einen neuen Kommentar zur Tauchveranstaltung \"" + args[1] + "\": " + args[2];
                case "es" -> args[0] + " añadió un nuevo comentario al evento de buceo \"" + args[1] + "\": " + args[2];
                default -> args[0] + " added a new comment to the dive event \"" + args[1] + "\": " + args[2];
            };
        });

        var response = commentService.createComment(1L, request);

        assertNotNull(response);
        assertEquals(999L, response.getId());

        var requestCaptor = ArgumentCaptor.forClass(io.oxalate.backend.api.request.MessageRequest.class);
        var recipientCaptor = ArgumentCaptor.forClass(Long.class);
        verify(messageService, times(3)).createEventCommentNotificationForUser(requestCaptor.capture(), recipientCaptor.capture());
        assertEquals(List.of(1L, 2L, 3L), recipientCaptor.getAllValues());
        assertEquals("New comment on Dive event 55", requestCaptor.getAllValues()
                                                                  .get(0)
                                                                  .getTitle());
        assertEquals("Anderson Alice added a new comment to the dive event \"Dive event 55\": Great dive", requestCaptor.getAllValues()
                                                                                                                        .get(0)
                                                                                                                        .getMessage());
        assertEquals("Neuer Kommentar zu Dive event 55", requestCaptor.getAllValues()
                                                                      .get(1)
                                                                      .getTitle());
        assertEquals("Anderson Alice hat einen neuen Kommentar zur Tauchveranstaltung \"Dive event 55\": Great dive", requestCaptor.getAllValues()
                                                                                                                                   .get(1)
                                                                                                                                   .getMessage());
        assertEquals("Nuevo comentario en Dive event 55", requestCaptor.getAllValues()
                                                                       .get(2)
                                                                       .getTitle());
        assertEquals("Anderson Alice añadió un nuevo comentario al evento de buceo \"Dive event 55\": Great dive", requestCaptor.getAllValues()
                                                                                                                                .get(2)
                                                                                                                                .getMessage());
    }

    @Test
    void createCommentOutsideDiveEventDoesNotSendNotificationsOk() {
        var author = buildUser(1L, "en", "Alice", "Anderson");
        var unrelatedParent = buildComment(800L, 1L, null, "Forum", "Forum");
        var savedComment = buildComment(999L, 1L, 800L, "Hello", "Hello body");
        var request = CommentRequest.builder()
                                    .title("Hello")
                                    .body("Hello body")
                                    .parentCommentId(800L)
                                    .commentType(CommentTypeEnum.USER_COMMENT)
                                    .build();

        when(userService.findUserEntityById(1L)).thenReturn(author);
        when(commentRepository.findById(800L)).thenReturn(Optional.of(unrelatedParent));
        when(commentRepository.findById(999L)).thenReturn(Optional.of(savedComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentRepository.countChildren(999L)).thenReturn(0L);
        when(eventCommentRepository.findByComment_Id(999L)).thenReturn(Optional.empty());
        when(eventCommentRepository.findByComment_Id(800L)).thenReturn(Optional.empty());
        when(avatarFileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(portalConfigurationService.getBooleanConfiguration(COMMENTING.group, COMMENT_REQUIRE_REVIEW.key)).thenReturn(false);

        var response = commentService.createComment(1L, request);

        assertNotNull(response);
        verify(messageService, never()).createEventCommentNotificationForUser(any(), anyLong());
    }
}
