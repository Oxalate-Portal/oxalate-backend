package io.oxalate.backend.repository;

import io.oxalate.backend.api.EmailNotificationTypeEnum;
import io.oxalate.backend.model.EmailNotificationSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailNotificationSubscriptionRepository extends ListCrudRepository<EmailNotificationSubscription, Long> {
    Optional<EmailNotificationSubscription> findByEmailNotificationTypeAndUserId(EmailNotificationTypeEnum emailNotificationTypeEnum, long userId);

    List<EmailNotificationSubscription> findByUserIdOrderByIdAsc(long userId);

    @Modifying
    void deleteById(long id);

    List<EmailNotificationSubscription> findByEmailNotificationType(EmailNotificationTypeEnum emailType);
}
