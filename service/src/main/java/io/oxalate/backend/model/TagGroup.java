package io.oxalate.backend.model;

import io.oxalate.backend.api.TagGroupEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tag_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "tagGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TagGroupTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "tagGroup")
    private Set<Tag> tags = new HashSet <>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type")
    private TagGroupEnum type;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Map<String, String> getTranslatedNames() {
        return translations.stream()
                           .collect(Collectors.toMap(
                                   TagGroupTranslation::getLanguage,
                                   TagGroupTranslation::getName,
                                   (existing, duplicate) -> existing // tolerate duplicates
                           ));
    }

    public String getNameInLanguage(String language) {
        return translations.stream()
                           .filter(t -> t.getLanguage()
                                         .equals(language))
                           .map(TagGroupTranslation::getName)
                           .findFirst()
                           .orElse(code);
    }
}
