package io.oxalate.backend.repository;

import io.oxalate.backend.model.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ListCrudRepository<User, Long>, CrudRepository<User, Long> {
    List<User> findAllByOrderByIdAsc();

    Optional<User> findById(long userId);

    @Query(nativeQuery = true, value = "SELECT u.* FROM users u WHERE u.username ILIKE :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query(nativeQuery = true, value =
            "SELECT u.* FROM events e, event_participants ep, users u " +
                    "WHERE e.id = :eventId AND e.id = ep.event_id AND ep.user_id = u.id " +
                    "AND ep.participant_type IN (:participantTypes)")
    Set<User> findEventParticipantsByTypes(@Param("eventId") long eventId, @Param("participantTypes") List<String> participantTypes);

    @Query(nativeQuery = true, value =
            "SELECT DISTINCT u.* FROM user_roles ur, users u WHERE u.id = ur.user_id AND ur.role_id = :roleId AND u.status <> 'ANONYMIZED'")
    List<User> findAllByRole(@Param("roleId") long roleId);

    @Query(nativeQuery = true, value = "UPDATE users SET  approved_terms = false WHERE users.status <> 'ANONYMIZED'")
    @Modifying
    void resetTermAnswer();

    List<User> findAllByFirstNameContainsIgnoreCaseOrLastNameContainsIgnoreCase(String firstName, String lastName);

    List<User> findByFirstNameContainsIgnoreCaseAndLastNameContainsIgnoreCase(String firstName, String lastName);
}
