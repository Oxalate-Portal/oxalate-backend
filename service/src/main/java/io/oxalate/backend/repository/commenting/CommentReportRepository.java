package io.oxalate.backend.repository.commenting;

import io.oxalate.backend.api.ReportStatusEnum;
import io.oxalate.backend.model.commenting.CommentReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    boolean existsByUserIdAndCommentId(long userId, long commentId);

    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.commentId = ?1")
    long countByCommentId(long commentId);

    CommentReport findByUserIdAndCommentId(long userId, long commentId);

    List<CommentReport> findAllByStatus(ReportStatusEnum reportStatusEnum);

    List<CommentReport> findAllByCommentId(long commentId);
}
