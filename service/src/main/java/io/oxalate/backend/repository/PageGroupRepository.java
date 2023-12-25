package io.oxalate.backend.repository;

import io.oxalate.backend.model.PageGroup;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageGroupRepository extends CrudRepository<PageGroup, Long> {
    List<PageGroup> findAllById(long pageGroupId);
    @Query("SELECT pg FROM PageGroup pg WHERE pg.id != ?1")
    List<PageGroup> findAllExceptId(long pageGroupId);
    List<PageGroup> findByIdIn(List<Long> pageGroupIdList);
}
