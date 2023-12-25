package io.oxalate.backend.repository;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.model.PageRoleAccess;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageRoleAccessRepository extends CrudRepository<PageRoleAccess, Long> {
    Set<PageRoleAccess> findByPageIdAndRoleIn(Long pageId, Set<RoleEnum> roles);

    Set<PageRoleAccess> findByPageIdAndRoleInAndWritePermission(long pageId, Set<RoleEnum> role, boolean writePermission);

    @Modifying
    void deleteAllByPageId(Long pageId);

    Set<PageRoleAccess> findAllByPageId(Long id);
}
