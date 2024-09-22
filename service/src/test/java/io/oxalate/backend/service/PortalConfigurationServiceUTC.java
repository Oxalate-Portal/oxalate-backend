package io.oxalate.backend.service;

import io.oxalate.backend.model.PortalConfiguration;
import io.oxalate.backend.repository.PortalConfigurationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalConfigurationServiceUTC {

    @Mock
    private PortalConfigurationRepository portalConfigurationRepository;

    @InjectMocks
    private PortalConfigurationService portalConfigurationService;

    @BeforeEach
    void setUp() {
        setupPortalConfigurationRepository();
        portalConfigurationService.postConstruct();
    }

    @Test
    void getStringConfiguration() {
        var value = portalConfigurationService.getStringConfiguration("group-1", "key-string");
        assertNotNull(value);
        assertEquals(String.class, value.getClass());
        assertEquals("a string", value);
    }

    @Test
    void getBooleanConfiguration() {
        var value = portalConfigurationService.getBooleanConfiguration("group-1", "key-boolean");
        assertTrue(value);
    }

    @Test
    void getNumericConfiguration() {
        var value = portalConfigurationService.getNumericConfiguration("group-1", "key-numeric");
        assertEquals(1L, value);
    }

    @Test
    void getArrayConfiguration() {
        var value = portalConfigurationService.getArrayConfiguration("group-1", "key-array");
        assertNotNull(value);
        assertEquals(3, value.size());
        assertEquals("one", value.get(0));
    }

    @Test
    void setRuntimeValue() {
        var group = "group-1";
        var key = "key-string";

        var portalConfiguration = PortalConfiguration.builder()
                                                     .id(1L)
                                                     .groupKey(group)
                                                     .settingKey(key)
                                                     .valueType("string")
                                                     .defaultValue("a string")
                                                     .runtimeValue(null)
                                                     .requiredRuntime(false)
                                                     .description("Description for group-1 type string")
                                                     .build();

        when(portalConfigurationRepository.findByGroupKeyAndSettingKey(group, key))
                .thenReturn(Optional.of(portalConfiguration));
        var updatedPortalConfiguration = portalConfiguration.toBuilder()
                                                            .runtimeValue("new value")
                                                            .build();
        when(portalConfigurationRepository.save(updatedPortalConfiguration)).thenReturn(updatedPortalConfiguration);

        portalConfigurationService.setRuntimeValue(group, key, "new value");

        verify(portalConfigurationRepository, times(1)).save(updatedPortalConfiguration);
        verify(portalConfigurationRepository, times(1)).findByGroupKeyAndSettingKey(group, key);

        var value = portalConfigurationService.getStringConfiguration("group-1", "key-string");
        assertEquals("new value", value);
    }

    private void setupPortalConfigurationRepository() {
        var portalConfigurationList = new ArrayList<PortalConfiguration>();

        for (int i = 1; i < 5; i++) {
            for (var type : List.of("string", "boolean", "numeric", "array")) {
                var portalconfiguration = PortalConfiguration.builder()
                                                             .groupKey("group-" + i)
                                                             .settingKey("key-" + type)
                                                             .valueType(type)
                                                             .defaultValue(getTypeValue(type))
                                                             .runtimeValue(null)
                                                             .requiredRuntime(false)
                                                             .description("Description for group-" + i + "  type " + type)
                                                             .build();
                portalConfigurationList.add(portalconfiguration);
            }
        }

        when(portalConfigurationRepository.findAll()).thenReturn(portalConfigurationList);
    }

    private String getTypeValue(String type) {
        return switch (type) {
            case "string" -> "a string";
            case "boolean" -> "true";
            case "numeric" -> "1";
            case "array" -> "one,two,three";
            default -> "";
        };
    }
}
