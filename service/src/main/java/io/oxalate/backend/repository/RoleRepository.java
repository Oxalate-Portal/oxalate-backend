package io.oxalate.backend.repository;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.model.Role;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {

    @NonNull
    Set<Role> findAll();

    Optional<Role> findByName(RoleEnum roleEnum);

    @Query(nativeQuery = true, value = "SELECT r.* FROM roles r, user_roles ur WHERE r.id = ur.role_id AND ur.user_id = :userId")
    Set<Role> findByUser(@Param("userId") long userId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM user_roles WHERE user_id = :userId")
    void removeUserRoles(@Param("userId") long userId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)")
    void addUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM user_roles WHERE user_id = :userId")
    void deleteAllUserRolesByUserId(@Param("userId") long userId);
}
