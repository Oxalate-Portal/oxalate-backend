package io.oxalate.backend.controller;

import static io.oxalate.backend.api.AuditLevel.ERROR;
import static io.oxalate.backend.api.AuditLevel.INFO;
import static io.oxalate.backend.api.AuditLevel.WARN;
import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_ADMIN;
import static io.oxalate.backend.api.RoleEnum.ROLE_ORGANIZER;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.api.response.PaymentStatusResponse;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_ADD_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_ADD_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_ADD_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_FAIL;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_OK;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_START;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_UNAUTHORIZED;
import static io.oxalate.backend.events.AppAuditMessages.PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_FAIL;
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
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.rest.PaymentAPI;
import io.oxalate.backend.service.PaymentService;
import io.oxalate.backend.service.UserService;
import io.oxalate.backend.tools.AuthTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PaymentController implements PaymentAPI {
    private final PaymentService paymentService;
    private final UserService userService;

    private static final String AUDIT_NAME = "PaymentController";
    private final AppEventPublisher appEventPublisher;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentStatusResponse>> getAllActivePaymentStatus(HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(PAYMENTS_GET_ALL_ACTIVE_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(PAYMENTS_GET_ALL_ACTIVE_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to reset all periodic payments without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var responseList = paymentService.getAllActivePaymentStatus();

        return appendNameToPaymentStatusResponse(request, auditUuid, responseList, PAYMENTS_GET_ALL_ACTIVE_FAIL, PAYMENTS_GET_ALL_ACTIVE_OK);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentStatusResponse>> getAllActivePaymentStatusOfType(PaymentTypeEnum paymentType, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_UNAUTHORIZED, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to reset all periodic payments without proper permission", AuthTools.getCurrentUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var responseList = paymentService.getAllActivePaymentByType(paymentType);

        return appendNameToPaymentStatusResponse(request, auditUuid, responseList, PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_FAIL,
                PAYMENTS_GET_ALL_ACTIVE_WITH_TYPE_OK);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatusForUser(long userId, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(PAYMENTS_GET_USER_STATUS_START, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        // Only organizers and admins can view other users info
        if (!AuthTools.currentUserHasAnyRole(ROLE_ORGANIZER, ROLE_ADMIN) && AuthTools.getCurrentUserId() != userId) {
            appEventPublisher.publishAuditEvent(PAYMENTS_GET_USER_STATUS_UNAUTHORIZED + userId, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(),
                    auditUuid);
            log.error("User ID {} tried to get user {} payment info", AuthTools.getCurrentUserId(), userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var response = paymentService.getPaymentStatusForUser(userId);
        appEventPublisher.publishAuditEvent(PAYMENTS_GET_USER_STATUS_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentStatusResponse> addPaymentForUser(PaymentRequest paymentRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(PAYMENTS_ADD_START + paymentRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(PAYMENTS_ADD_UNAUTHORIZED + paymentRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to add payment for user {} without proper permission", AuthTools.getCurrentUserId(), paymentRequest.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var response = paymentService.savePayment(paymentRequest);
        appEventPublisher.publishAuditEvent(PAYMENTS_ADD_OK + paymentRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentStatusResponse> updatePaymentForUser(PaymentRequest paymentRequest, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(PAYMENTS_UPDATE_START + paymentRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(PAYMENTS_UPDATE_UNAUTHORIZED + paymentRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to update payment for user {} without proper permission", AuthTools.getCurrentUserId(), paymentRequest.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(null);
        }

        var response = paymentService.savePayment(paymentRequest);

        if (response == null) {
            appEventPublisher.publishAuditEvent(PAYMENTS_UPDATE_FAIL + paymentRequest.getUserId(), ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        appEventPublisher.publishAuditEvent(PAYMENTS_UPDATE_OK + paymentRequest.getUserId(), INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetAllPayments(PaymentTypeEnum paymentType, HttpServletRequest request) {
        var auditUuid = appEventPublisher.publishAuditEvent(PAYMENTS_RESET_START + " " +  paymentType, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId());

        if (!AuthTools.currentUserHasAnyRole(ROLE_ADMIN)) {
            appEventPublisher.publishAuditEvent(PAYMENTS_RESET_UNAUTHORIZED + " " +  paymentType, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            log.error("User ID {} tried to reset all {} payments without proper permission", AuthTools.getCurrentUserId(), paymentType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        if (!paymentService.resetAllPayments(paymentType)) {
            appEventPublisher.publishAuditEvent(PAYMENTS_RESET_FAIL, ERROR, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        appEventPublisher.publishAuditEvent(PAYMENTS_RESET_OK, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    private ResponseEntity<List<PaymentStatusResponse>> appendNameToPaymentStatusResponse(HttpServletRequest request, UUID auditUuid,
            List<PaymentStatusResponse> responseList, String paymentsGetAllActiveWithTypeFail, String paymentsGetAllActiveWithTypeOk) {
        for (PaymentStatusResponse response : responseList) {
            var optionalUser = userService.findUserById(response.getUserId());

            if (optionalUser.isEmpty()) {
                appEventPublisher.publishAuditEvent(paymentsGetAllActiveWithTypeFail, WARN, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
                log.error("User ID {} from payments could not found", response.getUserId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            var user = optionalUser.get();
            response.setName(user.getLastName() + " " + user.getFirstName());
        }

        appEventPublisher.publishAuditEvent(paymentsGetAllActiveWithTypeOk, INFO, request, AUDIT_NAME, AuthTools.getCurrentUserId(), auditUuid);
        return ResponseEntity.status(HttpStatus.OK).body(responseList);
    }
}
