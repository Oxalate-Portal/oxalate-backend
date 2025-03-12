package io.oxalate.backend.repository.commenting;

import io.oxalate.backend.model.commenting.ForumTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumTopicRepository extends JpaRepository<ForumTopic, Long> {
}
