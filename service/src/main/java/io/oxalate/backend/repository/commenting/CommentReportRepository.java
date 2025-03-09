package io.oxalate.backend.repository.commenting;

import io.oxalate.backend.model.commenting.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    boolean existsByUserIdAndCommentId(long userId, long commentId);

    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.commentId = ?1")
    long countByCommentId(long commentId);

    CommentReport findByUserIdAndCommentId(long userId, long commentId);
}
