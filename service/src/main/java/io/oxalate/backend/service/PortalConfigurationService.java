package io.oxalate.backend.service;

import io.oxalate.backend.api.PortalConfigEnum;
import io.oxalate.backend.api.request.PortalConfigurationRequest;
import io.oxalate.backend.api.response.FrontendConfigurationResponse;
import io.oxalate.backend.api.response.PortalConfigurationResponse;
import io.oxalate.backend.model.PortalConfiguration;
import io.oxalate.backend.repository.PortalConfigurationRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PortalConfigurationService {
    private final PortalConfigurationRepository portalConfigurationRepository;
    private List<PortalConfiguration> portalConfigurations;

    @PostConstruct
    public void postConstruct() {
        this.portalConfigurations = portalConfigurationRepository.findAll();
        log.debug("Loaded {} portal configurations", portalConfigurations.size());
        log.debug("Loaded data: {}", portalConfigurations);
    }

    public List<PortalConfigurationResponse> getAllConfigurations() {
        var responses = new ArrayList<PortalConfigurationResponse>();

        for (var config : this.portalConfigurations) {
            responses.add(config.toResponse());
        }

        return responses;
    }

    public List<PortalConfigurationResponse> reloadPortalConfigurations() {
        log.debug("Reloading portal configurations");
        var newConfig = portalConfigurationRepository.findAll();
        log.debug("New configuration: {} portal configurations: {}", newConfig.size(), newConfig);
        this.portalConfigurations = newConfig;
        return getAllConfigurations();
    }

    public String getStringConfiguration(String group, String key) {
        var config = getPortalConfiguration(group, key);

        if (config.isPresent()) {
            if (config.get()
                      .getRuntimeValue() != null) {
                return config.get()
                             .getRuntimeValue();
            }

            return config.get()
                         .getDefaultValue();
        }

        log.error("Could not find configuration for group: {} and key: {}", group, key);
        return null;
    }

    public boolean getBooleanConfiguration(String group, String key) {
        var stringValue = getStringConfiguration(group, key);
        return stringValue.equals("true");
    }

    public long getNumericConfiguration(String group, String key) {
        var stringValue = getStringConfiguration(group, key);
        return Long.parseLong(stringValue);
    }

    public List<String> getArrayConfiguration(String group, String key) {
        var stringValue = getStringConfiguration(group, key);
        if (stringValue != null) {
            return List.of(stringValue.split(","));
        }

        return null;
    }

    @Transactional
    public void setRuntimeValue(String group, String key, String value) {
        var optionalPortalConfiguration = portalConfigurationRepository.findByGroupKeyAndSettingKey(group, key);

        if (optionalPortalConfiguration.isEmpty()) {
            log.error("Could not find configuration for group: {} and key: {}", group, key);
            return;
        }

        var portalConfiguration = optionalPortalConfiguration.get();

        portalConfiguration.setRuntimeValue(value);
        portalConfigurationRepository.save(portalConfiguration);
        var optionalRuntimeConfiguration = getPortalConfiguration(group, key);

        if (optionalRuntimeConfiguration.isPresent()) {
            optionalRuntimeConfiguration.get()
                                        .setRuntimeValue(value);
        } else {
            log.error("Could not set in-memory runtime configuration for group: {} and key: {}", group, key);
        }
    }

    @Transactional
    public void updateConfigurationValue(PortalConfigurationRequest portalConfigurationRequest) {
        var id = portalConfigurationRequest.getId();
        var value = portalConfigurationRequest.getValue() == "" ? null : portalConfigurationRequest.getValue();

        var optionalPortalConfiguration = portalConfigurationRepository.findById(id);

        if (optionalPortalConfiguration.isEmpty()) {
            log.error("Could not find configuration for id: {}", id);
            return;
        }

        var portalConfiguration = optionalPortalConfiguration.get();

        setRuntimeValue(portalConfiguration.getGroupKey(), portalConfiguration.getSettingKey(), value);
        reloadPortalConfigurations();
    }

    public List<FrontendConfigurationResponse> getFrontendConfigurations() {
        var frontendConfigurations = new ArrayList<FrontendConfigurationResponse>();

        // Get all configurations which are in group frontend and general
        for (var config : portalConfigurations) {
            if (config.getGroupKey()
                      .equals("frontend")
            || config.getGroupKey()
                    .equals("general")) {
                var effectiveValue = config.getRuntimeValue() != null ? config.getRuntimeValue() : config.getDefaultValue();
                frontendConfigurations.add(FrontendConfigurationResponse.builder()
                                                                        .key(config.getSettingKey())
                                                                        .value(effectiveValue)
                                                                        .build());
            }
        }

        return frontendConfigurations;
    }

    /**
     * Check if the given configuration is in effect. This only works for array configurations where the value is searched in the array.
     * @param portalConfigEnum
     * @param emailConfigEnum
     * @param setting
     * @return
     */
    public boolean isEnabled(PortalConfigEnum portalConfigEnum, PortalConfigEnum.EmailConfigEnum emailConfigEnum, String setting) {
        // Get the configuration value for the given group and key
        var configArray = getArrayConfiguration(portalConfigEnum.group, emailConfigEnum.key);
        return configArray.contains(setting);
    }

    protected Optional<PortalConfiguration> getPortalConfiguration(String group, String key) {
        return portalConfigurations.stream()
                                   .filter(c -> c.getGroupKey()
                                                 .equals(group)
                                           && c.getSettingKey()
                                               .equals(key))
                                   .findFirst();
    }
}
