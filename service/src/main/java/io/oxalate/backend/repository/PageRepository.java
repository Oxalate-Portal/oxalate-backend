package io.oxalate.backend.repository;

import io.oxalate.backend.model.Page;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    // Management endpoints
    List<Page> findAllByPageGroupIdOrderByIdAsc(long pageGroupId);

    List<Page> findAllByIdInOrderByIdAsc(List<Long> pageIdList);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE pages SET status = :pageStatusEnum WHERE id = :pageId
            """)
    void updateStatus(@Param("pageId") long pageId, @Param("pageStatusEnum") String pageStatusEnum);

    /**
     * Count blog articles accessible by roles with optional search filter (case-insensitive).
     */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(DISTINCT p.id)
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR LOWER(pv.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.ingress) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.body) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    long countBlogArticlesCaseInsensitive(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search);

    /**
     * Count blog articles accessible by roles with optional search filter (case-sensitive).
     */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(DISTINCT p.id)
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR pv.title LIKE CONCAT('%', :search, '%')
                   OR pv.ingress LIKE CONCAT('%', :search, '%')
                   OR pv.body LIKE CONCAT('%', :search, '%'))
            """)
    long countBlogArticlesCaseSensitive(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search);

    /**
     * Find blog articles accessible by roles with pagination, sorting and optional search filter (case-insensitive).
     * Sorting by createdAt DESC by default.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR LOWER(pv.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.ingress) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.body) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseInsensitiveOrderByCreatedAtDesc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination, sorting and optional search filter (case-insensitive).
     * Sorting by createdAt ASC.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR LOWER(pv.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.ingress) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.body) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.created_at ASC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseInsensitiveOrderByCreatedAtAsc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination and optional search filter (case-sensitive).
     * Sorting by createdAt DESC by default.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR pv.title LIKE CONCAT('%', :search, '%')
                   OR pv.ingress LIKE CONCAT('%', :search, '%')
                   OR pv.body LIKE CONCAT('%', :search, '%'))
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseSensitiveOrderByCreatedAtDesc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination and optional search filter (case-sensitive).
     * Sorting by createdAt ASC.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR pv.title LIKE CONCAT('%', :search, '%')
                   OR pv.ingress LIKE CONCAT('%', :search, '%')
                   OR pv.body LIKE CONCAT('%', :search, '%'))
            ORDER BY p.created_at ASC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseSensitiveOrderByCreatedAtAsc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination, sorting by title (case-insensitive search).
     * Sorting by title DESC.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*, pv.title AS sort_title
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR LOWER(pv.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.ingress) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.body) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY sort_title DESC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseInsensitiveOrderByTitleDesc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination, sorting by title (case-insensitive search).
     * Sorting by title ASC.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*, pv.title AS sort_title
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR LOWER(pv.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.ingress) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(pv.body) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY sort_title ASC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseInsensitiveOrderByTitleAsc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination, sorting by title (case-sensitive search).
     * Sorting by title DESC.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*, pv.title AS sort_title
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR pv.title LIKE CONCAT('%', :search, '%')
                   OR pv.ingress LIKE CONCAT('%', :search, '%')
                   OR pv.body LIKE CONCAT('%', :search, '%'))
            ORDER BY sort_title DESC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseSensitiveOrderByTitleDesc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Find blog articles accessible by roles with pagination, sorting by title (case-sensitive search).
     * Sorting by title ASC.
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT p.*, pv.title AS sort_title
            FROM pages p
            JOIN page_versions pv ON p.id = pv.page_id
            JOIN page_role_access pra ON p.id = pra.page_id
            WHERE p.page_group_id = :pageGroupId
              AND p.status = 'PUBLISHED'
              AND pv.language = :language
              AND pra.role IN (:roles)
              AND pra.read_permission = true
              AND (:search IS NULL OR :search = ''
                   OR pv.title LIKE CONCAT('%', :search, '%')
                   OR pv.ingress LIKE CONCAT('%', :search, '%')
                   OR pv.body LIKE CONCAT('%', :search, '%'))
            ORDER BY sort_title ASC
            LIMIT :limit OFFSET :offset
            """)
    List<Page> findBlogArticlesCaseSensitiveOrderByTitleAsc(
            @Param("pageGroupId") long pageGroupId,
            @Param("language") String language,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
