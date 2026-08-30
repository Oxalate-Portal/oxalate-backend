package io.oxalate.backend.model;

import io.oxalate.backend.api.response.CertificateClassificationResponse;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificate_classifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateClassification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "classification_order", nullable = false)
    private Integer order;

    @Builder.Default
    @OneToMany(mappedBy = "classification", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CertificateClassificationTranslation> translations = new HashSet<>();

    public CertificateClassificationResponse toResponse() {
        return CertificateClassificationResponse.builder()
                                                .id(id)
                                                .description(description)
                                                .order(order)
                                                .titles(translations.stream()
                                                                    .collect(Collectors.toMap(
                                                                            CertificateClassificationTranslation::getLanguage,
                                                                            CertificateClassificationTranslation::getTitle,
                                                                            (first, duplicate) -> first)))
                                                .build();
    }

    public String getTitleInLanguage(String language) {
        if (translations == null || translations.isEmpty())
            return null;
        var requested = language == null ? null : language.toLowerCase(Locale.ROOT);
        return translations.stream()
                           .filter(t -> requested != null && requested.equals(t.getLanguage()
                                                                               .toLowerCase(Locale.ROOT)))
                           .map(CertificateClassificationTranslation::getTitle)
                           .findFirst()
                           .orElseGet(() -> translations.stream()
                                                        .filter(t -> "en".equalsIgnoreCase(t.getLanguage()))
                                                        .map(CertificateClassificationTranslation::getTitle)
                                                        .findFirst()
                                                        .orElse(translations.iterator()
                                                                            .next()
                                                                            .getTitle()));
    }
}
