package io.oxalate.backend.model;

import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.api.response.EmailNotificationSubscriptionResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "email_notification_subscriptions")
public class EmailNotificationSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @NotNull
    @Column(name = "user_id")
    private long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private EmailNotificationTypeEnum emailNotificationType;

    public EmailNotificationSubscriptionResponse toResponse() {
        return EmailNotificationSubscriptionResponse.builder()
                                                    .id(this.getId())
                                                    .emailNotificationType(this.getEmailNotificationType())
                                                    .userId(this.getUserId())
                                                    .build();
    }
}
