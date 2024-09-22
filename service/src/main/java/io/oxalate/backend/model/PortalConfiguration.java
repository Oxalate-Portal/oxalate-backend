package io.oxalate.backend.model;

import io.oxalate.backend.api.response.PortalConfigurationResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "portal_configuration", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "group_key", "setting_key" })
})
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PortalConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_key", nullable = false)
    private String groupKey;

    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @Column(name = "value_type", nullable = false)
    private String valueType;

    @Column(name = "default_value", nullable = false)
    private String defaultValue;

    @Column(name = "runtime_value")
    private String runtimeValue;

    @Column(name = "required_runtime", nullable = false)
    private Boolean requiredRuntime;

    @Column(name = "description", nullable = false)
    private String description;

    public PortalConfigurationResponse toResponse() {
        return PortalConfigurationResponse.builder()
                                          .id(this.id)
                                          .groupKey(this.groupKey)
                                          .settingKey(this.settingKey)
                                          .valueType(this.valueType)
                                          .defaultValue(this.defaultValue)
                                          .runtimeValue(this.runtimeValue)
                                          .requiredRuntime(this.requiredRuntime)
                                          .description(this.description)
                                          .build();
    }
}
