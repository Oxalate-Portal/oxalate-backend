package io.oxalate.backend.repository;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.model.Membership;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUserId(long userId);

    @Query("""
            SELECT m
            FROM Membership m
            WHERE m.status = :status
              AND (m.endDate >= CURRENT_TIMESTAMP
                   OR m.endDate IS NULL)
            ORDER BY m.userId
            """)
    List<Membership> findAllCurrentAndFutureByStatus(MembershipStatusEnum status);

    @Query("""
            SELECT m
            FROM Membership m
            WHERE m.userId = :userId
              AND m.status = 'ACTIVE'
              AND (m.endDate >= CURRENT_TIMESTAMP
                   OR m.endDate IS NULL)
            ORDER BY m.startDate DESC
            """)
    List<Membership> findAllCurrentAndFutureActiveByUserId(long userId);
}
