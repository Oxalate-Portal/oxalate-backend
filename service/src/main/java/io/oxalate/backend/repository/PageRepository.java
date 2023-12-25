package io.oxalate.backend.repository;

import io.oxalate.backend.model.Page;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageRepository extends CrudRepository<Page, Long> {

    // Management endpoints
    List<Page> findAllByPageGroupIdOrderByIdAsc(long pageGroupId);

    List<Page> findAllByIdInOrderByIdAsc(List<Long> pageIdList);

    List<Page> findAllByIdOrderByIdAsc(long pageId);

    void deleteAllById(long id);
}
