package io.oxalate.backend.controller;

import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.PaymentStatusResponse;
import io.oxalate.backend.audit.AuditSource;
import io.oxalate.backend.audit.Audited;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_ADD_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_ADD_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_ADD_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_USER_STATUS_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_USER_STATUS_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_USER_STATUS_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_RESET_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_RESET_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_RESET_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_RESET_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_UPDATE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_UPDATE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_UPDATE_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_UPDATE_UNAUTHORIZED;
import io.oxalate.backend.exception.OxalateUnauthorizedException;
import io.oxalate.backend.exception.OxalateValidationException;
import io.oxalate.backend.rest.PaymentAPI;
import io.oxalate.backend.service.PaymentService;
import io.oxalate.backend.tools.AuthTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@AuditSource("PaymentController")
public class PaymentController implements PaymentAPI {
    private final PaymentService paymentService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PAYMENTS_GET_ALL_ACTIVE_START, okMessage = PAYMENTS_GET_ALL_ACTIVE_OK)
    public ResponseEntity<List<PaymentStatusResponse>> getAllActivePaymentStatus() {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.error("User ID {} tried to reset all periodic payments without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(PAYMENTS_GET_ALL_ACTIVE_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var responseList = paymentService.getAllActivePaymentStatus();
        paymentService.appendNameToPaymentStatusResponse(responseList);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(responseList);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_START, okMessage = PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_OK)
    public ResponseEntity<List<PaymentStatusResponse>> getAllActivePaymentStatusOfType(PaymentTypeEnum paymentType) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.error("User ID {} tried to reset all periodic payments without proper permission", AuthTools.getCurrentUserId());
            throw new OxalateUnauthorizedException(PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }

        var responseList = paymentService.getAllActivePaymentByType(paymentType);
        paymentService.appendNameToPaymentStatusResponse(responseList);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(responseList);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    @Audited(startMessage = PAYMENTS_GET_USER_STATUS_START, okMessage = PAYMENTS_GET_USER_STATUS_OK)
    public ResponseEntity<PaymentStatusResponse> getPaymentStatusForUser(long userId) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && AuthTools.getCurrentUserId() != userId) {
            log.error("User ID {} tried to get user {} payment info", AuthTools.getCurrentUserId(), userId);
            throw new OxalateUnauthorizedException(PAYMENTS_GET_USER_STATUS_UNAUTHORIZED + userId, HttpStatus.NOT_FOUND);
        }

        var response = paymentService.getPaymentStatusForUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PAYMENTS_ADD_START, okMessage = PAYMENTS_ADD_OK)
    public ResponseEntity<PaymentResponse> addPaymentForUser(PaymentRequest paymentRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.error("User ID {} tried to add payment for user {} without proper permission", AuthTools.getCurrentUserId(), paymentRequest.getUserId());
            throw new OxalateUnauthorizedException(PAYMENTS_ADD_UNAUTHORIZED + paymentRequest.getUserId(), HttpStatus.NOT_FOUND);
        }

        var response = paymentService.savePayment(paymentRequest);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PAYMENTS_UPDATE_START, okMessage = PAYMENTS_UPDATE_OK)
    public ResponseEntity<PaymentResponse> updatePaymentForUser(PaymentRequest paymentRequest) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.error("User ID {} tried to update payment for user {} without proper permission", AuthTools.getCurrentUserId(), paymentRequest.getUserId());
            throw new OxalateUnauthorizedException(PAYMENTS_UPDATE_UNAUTHORIZED + paymentRequest.getUserId(), HttpStatus.NOT_FOUND);
        }

        var response = paymentService.savePayment(paymentRequest);

        if (response == null) {
            throw new OxalateValidationException(PAYMENTS_UPDATE_FAIL + paymentRequest.getUserId(), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(startMessage = PAYMENTS_RESET_START, okMessage = PAYMENTS_RESET_OK)
    public ResponseEntity<Void> resetAllPayments(PaymentTypeEnum paymentType) {
        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            log.error("User ID {} tried to reset all {} payments without proper permission", AuthTools.getCurrentUserId(), paymentType);
            throw new OxalateUnauthorizedException(PAYMENTS_RESET_UNAUTHORIZED + " " + paymentType, HttpStatus.NOT_FOUND);
        }

        if (!paymentService.resetAllPayments(paymentType)) {
            throw new OxalateValidationException(PAYMENTS_RESET_FAIL, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
