package io.oxalate.backend.repository;

import io.oxalate.backend.model.ThirdPartyToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartyTokenRepository extends JpaRepository<ThirdPartyToken, Long> {
    Optional<ThirdPartyToken> findByTokenValue(String tokenValue);

    List<ThirdPartyToken> findAllByOrderByCreatedAtDesc();
}
