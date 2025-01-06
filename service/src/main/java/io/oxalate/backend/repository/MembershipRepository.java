package io.oxalate.backend.repository;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.model.Membership;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends ListCrudRepository<Membership, Long> {
    List<Membership> findByUserId(long userId);

    List<Membership> findAllByStatus(MembershipStatusEnum status);
}
