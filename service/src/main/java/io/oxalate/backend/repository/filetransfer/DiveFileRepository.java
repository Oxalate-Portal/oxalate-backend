package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.filetransfer.DiveFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DiveFileRepository extends JpaRepository<DiveFile, Long>, JpaSpecificationExecutor<DiveFile> {
    List<DiveFile> findByDiveGroupId(Long diveGroupId);
}
