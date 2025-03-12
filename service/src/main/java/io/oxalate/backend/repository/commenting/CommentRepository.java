package io.oxalate.backend.repository.commenting;

import io.oxalate.backend.model.commenting.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByParentCommentId(long parentCommentId);

    List<Comment> findAllByUserId(long userId);

    @NativeQuery(value = """
            WITH RECURSIVE comment_tree AS (
                SELECT id, parent_comment_id
                FROM comments
                WHERE id = :parentId

                UNION ALL

                SELECT c.id, c.parent_comment_id
                FROM comments c
                INNER JOIN comment_tree ct ON c.parent_comment_id = ct.id
            )
            SELECT COUNT(*) - 1 AS total_children
            FROM comment_tree""")
    long countChildren(@Param(value = "parentId") long commentId);

	Comment findByTitle(@NonNull String eventRootTopic);
}
