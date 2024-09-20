package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.DiveFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiveFileRepository extends ListCrudRepository<DiveFile, Long> {
    Optional<DiveFile> findByFileNameAndDiveGroupId(String fileName, Long diveGroupId);
    List<DiveFile> findByDiveGroupId(Long diveGroupId);
}
