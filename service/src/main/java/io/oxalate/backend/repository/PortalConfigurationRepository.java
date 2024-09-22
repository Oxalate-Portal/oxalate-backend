package io.oxalate.backend.repository;

import io.oxalate.backend.model.PortalConfiguration;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortalConfigurationRepository extends ListCrudRepository<PortalConfiguration, Long> {
    Optional<PortalConfiguration> findByGroupKeyAndSettingKey(String groupKey, String settingKey);
}
