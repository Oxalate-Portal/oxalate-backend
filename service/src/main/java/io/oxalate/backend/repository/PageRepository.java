package io.oxalate.backend.repository;

import io.oxalate.backend.api.PageStatusEnum;
import io.oxalate.backend.model.Page;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PageRepository extends CrudRepository<Page, Long> {

    // Management endpoints
    List<Page> findAllByPageGroupIdOrderByIdAsc(long pageGroupId);

    List<Page> findAllByIdInOrderByIdAsc(List<Long> pageIdList);

    @Query(nativeQuery = true, value = "UPDATE pages SET status = :pageStatusEnum WHERE id = :pageId")
    void updateStatus(@Param("pageId") long pageId, @Param("pageStatusEnum") PageStatusEnum pageStatusEnum);
}
