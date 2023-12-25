package io.oxalate.backend.model;

import io.oxalate.backend.api.response.MessageResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "description")
    private String description;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "creator")
    private long creator;

    @Column(name = "created_at")
    private Instant createdAt;

    public MessageResponse toMessageResponse() {
        return MessageResponse.builder()
                .id(id)
                .description(description)
                .title(title)
                .message(message)
                .creator(creator)
                .createdAt(createdAt)
                .build();
    }
}
