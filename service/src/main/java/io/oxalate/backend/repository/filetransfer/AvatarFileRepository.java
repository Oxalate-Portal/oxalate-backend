package io.oxalate.backend.repository.filetransfer;

import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.AvatarFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AvatarFileRepository extends JpaRepository<AvatarFile, Long>, JpaSpecificationExecutor<AvatarFile> {
    Optional<AvatarFile> findByFileName(String fileName);
    Optional<AvatarFile> findByCreator(User creator);

    @Query(nativeQuery = true, value = "SELECT * FROM avatar_files WHERE creator = ?1")
    Optional<AvatarFile> findByUserId(long userId);
}
