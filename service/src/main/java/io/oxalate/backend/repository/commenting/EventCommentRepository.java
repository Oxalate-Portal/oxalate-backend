package io.oxalate.backend.repository.commenting;

import io.oxalate.backend.model.commenting.EventComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCommentRepository extends JpaRepository<EventComment, Long> {}
