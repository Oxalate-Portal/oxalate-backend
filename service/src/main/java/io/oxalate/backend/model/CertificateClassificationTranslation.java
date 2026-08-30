package io.oxalate.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificate_classification_translations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateClassificationTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    private CertificateClassification classification;

    @Column(name = "language", nullable = false, length = 2)
    private String language;

    @Column(name = "title", nullable = false)
    private String title;
}
