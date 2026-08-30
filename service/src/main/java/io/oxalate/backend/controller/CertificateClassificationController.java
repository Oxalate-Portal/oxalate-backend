package io.oxalate.backend.controller;

import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import io.oxalate.backend.api.request.CertificateClassificationRequest;
import io.oxalate.backend.api.response.CertificateClassificationResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK;
import static io.oxalate.backend.events.AppAuditMessages.CERTIFICATE_CLASSIFICATION_MANAGEMENT_START;
import io.oxalate.backend.exception.OxalateNotFoundException;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.rest.CertificateClassificationAPI;
import io.oxalate.backend.service.CertificateService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@AuditSource("CertificateClassificationController")
public class CertificateClassificationController implements CertificateClassificationAPI {
    private final CertificateService certificateService;

    private void verifyAdmin() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            throw new OxalateUnauthorizedException("User is not authorized to manage certificate classifications", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_START, okMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK)
    public ResponseEntity<List<CertificateClassificationResponse>> getAll() {
        verifyAdmin();
        return ResponseEntity.ok(certificateService.findAllClassifications());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_START, okMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK)
    public ResponseEntity<CertificateClassificationResponse> getById(long id) {
        verifyAdmin();
        var response = certificateService.findClassification(id);
        if (response == null)
            throw new OxalateNotFoundException("Certificate classification not found: " + id);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_START, okMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK)
    public ResponseEntity<CertificateClassificationResponse> create(CertificateClassificationRequest request) {
        verifyAdmin();
        var response = certificateService.saveClassification(request);
        if (response == null)
            return ResponseEntity.badRequest()
                                 .build();
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_START, okMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK)
    public ResponseEntity<CertificateClassificationResponse> update(CertificateClassificationRequest request) {
        verifyAdmin();
        var response = certificateService.saveClassification(request);
        if (response == null)
            throw new OxalateNotFoundException("Certificate classification not found: " + request.getId());
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_START, okMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK)
    public ResponseEntity<Void> reorder(List<CertificateClassificationRequest> requests) {
        verifyAdmin();
        try {
            certificateService.reorderClassifications(requests);
            return ResponseEntity.ok()
                                 .build();
        } catch (IllegalArgumentException e) {
            throw new OxalateValidationException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_START, okMessage = CERTIFICATE_CLASSIFICATION_MANAGEMENT_OK)
    public ResponseEntity<Void> delete(long id) {
        verifyAdmin();
        if (!certificateService.deleteClassification(id))
            throw new OxalateNotFoundException("Certificate classification not found: " + id);
        return ResponseEntity.ok()
                             .build();
    }
}
