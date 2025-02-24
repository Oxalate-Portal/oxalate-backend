package io.oxalate.backend.repository.commenting;

import io.oxalate.backend.model.commenting.PageComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageCommentRepository extends JpaRepository<PageComment, Long> {}
