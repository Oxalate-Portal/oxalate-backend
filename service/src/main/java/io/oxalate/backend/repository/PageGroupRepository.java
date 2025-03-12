package io.oxalate.backend.repository;

import io.oxalate.backend.model.PageGroup;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PageGroupRepository extends CrudRepository<PageGroup, Long> {
    List<PageGroup> findAllById(long pageGroupId);
    @Query(nativeQuery = true, value = "SELECT * FROM page_groups pg WHERE pg.id != ?1")
    List<PageGroup> findAllExceptId(long pageGroupId);
    List<PageGroup> findByIdIn(List<Long> pageGroupIdList);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE page_groups SET status = :pageStatusEnum WHERE id = :pageGroupId")
    void updateStatus(@Param("pageGroupId") long pageGroupId, @Param("pageStatusEnum") String pageStatusEnum);
}
