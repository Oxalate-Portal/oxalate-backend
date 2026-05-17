package io.oxalate.backend.api;

import io.oxalate.backend.api.request.ConfirmationRequest;
import io.oxalate.backend.api.request.EventSubscribeRequest;
import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.request.commenting.CommentRequest;
import io.oxalate.backend.api.response.UserUpdateStatus;
import io.oxalate.backend.api.response.commenting.CommentReportResponse;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DtoConstructionUTC {

    @Test
    void ConfirmationRequestBuilderOk() {
        var dto = ConfirmationRequest.builder()
                                     .confirmationAnswer(true)
                                     .build();
        assertTrue(dto.isConfirmationAnswer());
    }

    @Test
    void ConfirmationRequestConstructorOk() {
        var dto = new ConfirmationRequest(true);
        assertTrue(dto.isConfirmationAnswer());
        assertNotNull(new ConfirmationRequest());
    }

    @Test
    void EventSubscribeRequestBuilderOk() {
        var dto = EventSubscribeRequest.builder()
                                       .diveEventId(10L)
                                       .userType(UserTypeEnum.SCUBA_DIVER)
                                       .build();
        assertEquals(10L, dto.getDiveEventId());
        assertEquals(UserTypeEnum.SCUBA_DIVER, dto.getUserType());
    }

    @Test
    void EventSubscribeRequestConstructorOk() {
        var dto = new EventSubscribeRequest(11L, UserTypeEnum.FREE_DIVER);
        assertEquals(11L, dto.getDiveEventId());
        assertEquals(UserTypeEnum.FREE_DIVER, dto.getUserType());
        assertNotNull(new EventSubscribeRequest());
    }

    @Test
    void MessageRequestBuilderOk() {
        var dto = MessageRequest.builder()
                                .id(1L)
                                .title("title")
                                .message("message")
                                .creator(2L)
                                .recipients(List.of(3L, 4L))
                                .sendAll(false)
                                .build();

        assertEquals("title", dto.getTitle());
        assertEquals(List.of(3L, 4L), dto.getRecipients());
        assertEquals(false, dto.getSendAll());
    }

    @Test
    void MessageRequestConstructorOk() {
        var dto = new MessageRequest(List.of(5L), true);
        assertEquals(List.of(5L), dto.getRecipients());
        assertEquals(true, dto.getSendAll());
        assertNotNull(new MessageRequest());
    }

    @Test
    void SignupRequestBuilderOk() {
        var dto = SignupRequest.builder()
                               .username("user@example.com")
                               .password("Secret123!")
                               .firstName("John")
                               .lastName("Doe")
                               .phoneNumber("12345")
                               .privacy(true)
                               .approvedTerms(true)
                               .language("en")
                               .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                               .build();

        assertEquals("user@example.com", dto.getUsername());
        assertEquals(UserTypeEnum.SCUBA_DIVER, dto.getPrimaryUserType());
    }

    @Test
    void SignupRequestConstructorOk() {
        var dto = new SignupRequest();
        dto.setUsername("setter@example.com");
        assertEquals("setter@example.com", dto.getUsername());
        assertNotNull(new SignupRequest("u", "p", "f", "l", "1", true, "n", true, 1L, "en", UserTypeEnum.FREE_DIVER));
    }

    @Test
    void CommentRequestBuilderOk() {
        var dto = CommentRequest.builder()
                                .id(1L)
                                .title("Title")
                                .body("Body")
                                .parentCommentId(10L)
                                .commentType(CommentTypeEnum.TOPIC)
                                .commentStatus(CommentStatusEnum.PUBLISHED)
                                .cancelReason("none")
                                .build();

        assertEquals("Title", dto.getTitle());
        assertEquals(CommentTypeEnum.TOPIC, dto.getCommentType());
    }

    @Test
    void CommentRequestConstructorOk() {
        var dto = new CommentRequest(1L, "Title", "Body", 10L, CommentTypeEnum.USER_COMMENT, CommentStatusEnum.DRAFTED, "reason");
        assertEquals("Body", dto.getBody());
        assertEquals(CommentStatusEnum.DRAFTED, dto.getCommentStatus());
        assertNotNull(new CommentRequest());
    }

    @Test
    void UserUpdateStatusBuilderOk() {
        var dto = UserUpdateStatus.builder()
                                  .status(UpdateStatusEnum.OK)
                                  .message("done")
                                  .build();
        assertEquals(UpdateStatusEnum.OK, dto.getStatus());
        assertEquals("done", dto.getMessage());
    }

    @Test
    void UserUpdateStatusConstructorOk() {
        var dto = new UserUpdateStatus(UpdateStatusEnum.FAIL, "failed");
        assertEquals(UpdateStatusEnum.FAIL, dto.getStatus());
        assertEquals("failed", dto.getMessage());
        assertNotNull(new UserUpdateStatus());
    }

    @Test
    void CommentReportResponseBuilderOk() {
        var dto = CommentReportResponse.builder()
                                       .id(1L)
                                       .reporter("reporter")
                                       .reporterId(2L)
                                       .reason("reason")
                                       .status(ReportStatusEnum.PENDING)
                                       .build();

        assertEquals("reporter", dto.getReporter());
        assertEquals(ReportStatusEnum.PENDING, dto.getStatus());
    }

    @Test
    void CommentReportResponseConstructorOk() {
        var dto = new CommentReportResponse(1L, "reporter", 2L, "reason", null, ReportStatusEnum.APPROVED);
        assertEquals(2L, dto.getReporterId());
        assertEquals(ReportStatusEnum.APPROVED, dto.getStatus());
        assertNotNull(new CommentReportResponse());
    }
}

