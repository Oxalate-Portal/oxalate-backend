package io.oxalate.backend.service;

import io.oxalate.backend.api.request.MessageRequest;
import io.oxalate.backend.api.response.MessageResponse;
import io.oxalate.backend.model.Message;
import io.oxalate.backend.repository.MessageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageResponse save(MessageRequest messageRequest) {

        var message = Message.builder()
                .description(messageRequest.getDescription())
                .title(messageRequest.getTitle())
                .message(messageRequest.getMessage())
                .creator(messageRequest.getCreator())
                .createdAt(Instant.now())
                .build();

        message = messageRepository.save(message);
        return message.toMessageResponse();
    }

    public List<MessageResponse> unreadUserMessages(long userId) {
        var unreadMessages = messageRepository.findUnreadUserMessages(userId);

        var messageResponses = new ArrayList<MessageResponse>();

        for (Message message : unreadMessages) {
            messageResponses.add(message.toMessageResponse());
        }

        return messageResponses;
    }

    @Transactional
    public void setMessageAsRead(long messageId, long userId) {
        messageRepository.setMessageAsRead(messageId, userId);
    }
}
