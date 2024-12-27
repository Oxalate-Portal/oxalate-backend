package io.oxalate.backend.model;

import io.oxalate.backend.api.response.CertificateResponse;
import io.oxalate.backend.api.response.download.DownloadCertificateResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "organization")
    private String organization;

    @Column(name = "certificate_name")
    private String certificateName;

    @Column(name = "certificate_id")
    private String certificateId;

    @Column(name = "diver_id")
    private String diverId;

    @Column(name = "certification_date")
    private Instant certificationDate;

    public CertificateResponse toCertificateResponse() {
        return CertificateResponse.builder()
                                  .id(this.id)
                                  .userId(this.userId)
                                  .organization(this.organization)
                                  .certificateName(this.certificateName)
                                  .certificateId(this.certificateId)
                                  .diverId(this.diverId)
                                  .certificationDate(this.certificationDate)
                                  .build();
    }

    public DownloadCertificateResponse toDownloadCertificateResponse() {
        return DownloadCertificateResponse.builder()
                                          .id(this.id)
                                          .userId(this.userId)
                                          .organization(this.organization)
                                          .certificateName(this.certificateName)
                                          .certificateId(this.certificateId)
                                          .diverId(this.diverId)
                                          .certificationDate(this.certificationDate)
                                          .memberName(null)
                                          .build();
    }
}
