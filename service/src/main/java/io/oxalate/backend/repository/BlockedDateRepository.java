package io.oxalate.backend.repository;

import io.oxalate.backend.model.BlockedDate;
import java.sql.Date;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedDateRepository extends CrudRepository<BlockedDate, Long> {

    List<BlockedDate> findAllByBlockedDateAfterOrderByBlockedDateAsc(Date currentDate);
}
