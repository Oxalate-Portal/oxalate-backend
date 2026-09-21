package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.DiveFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiveFileRepository extends JpaRepository<DiveFile, Long>, JpaSpecificationExecutor<DiveFile> {
    Optional<DiveFile> findByFileNameAndDiveGroupId(String fileName, Long diveGroupId);
    List<DiveFile> findByDiveGroupId(Long diveGroupId);
}
