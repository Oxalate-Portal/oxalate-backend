package io.oxalate.backend.service;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import static io.oxalate.backend.api.MembershipTypeEnum.PERIODICAL;
import io.oxalate.backend.api.PortalConfigEnum;
import static io.oxalate.backend.api.PortalConfigEnum.MembershipConfigEnum.MEMBERSHIP_PERIOD_START_POINT;
import static io.oxalate.backend.api.PortalConfigEnum.MembershipConfigEnum.MEMBERSHIP_PERIOD_UNIT;
import static io.oxalate.backend.api.PortalConfigEnum.MembershipConfigEnum.MEMBERSHIP_TYPE;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.PAYMENT_PERIOD_START;
import io.oxalate.backend.api.request.MembershipRequest;
import io.oxalate.backend.api.response.MembershipResponse;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.PeriodResult;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.tools.PeriodTool;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final PortalConfigurationService portalConfigurationService;
    private static final String MEMBERSHIP_DISABLED_WARNING = "Membership creation is disabled";
    private final UserService userService;

    public List<MembershipResponse> getAllActiveMemberships() {
        var membershipType = getMembershipTypeSetting();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return new ArrayList<>();
        }

        var memberships = membershipRepository.findAllByStatus(MembershipStatusEnum.ACTIVE);
        return memberships.stream()
                          .map(Membership::toResponse)
                          .collect(Collectors.toList());
    }

    public MembershipResponse findById(long membershipId) {
        var membershipType = getMembershipTypeSetting();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return MembershipResponse.builder()
                                     .build();
        }

        var optionalMembership = membershipRepository.findById(membershipId);

        if (optionalMembership.isEmpty()) {
            log.error("Could not find membership for id: {}", membershipId);
            return MembershipResponse.builder()
                                     .build();
        }

        return optionalMembership.get()
                                 .toResponse();
    }

    public List<MembershipResponse> getMembershipsForUser(long userId) {
        var membershipType = getMembershipTypeSetting();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return new ArrayList<>();
        }

        List<Membership> memberships = membershipRepository.findByUserId(userId);
        return memberships.stream()
                          .map(Membership::toResponse)
                          .collect(Collectors.toList());
    }

    @Transactional
    public MembershipResponse createMembership(MembershipRequest membershipRequest) {
        var membershipType = getMembershipTypeSetting();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return MembershipResponse.builder()
                                     .build();
        }

        // Check that the user does not already have an active membership, only one membership is allowed at a time
        var activeMemberships = membershipRepository.findByUserId(membershipRequest.getUserId())
                                                    .stream()
                                                    .filter(membership -> membership.getStatus()
                                                                                    .equals(MembershipStatusEnum.ACTIVE) && membership.getEndDate()
                                                                                                                                      .isAfter(LocalDate.now()))
                                                    .collect(Collectors.toList());

        if (!activeMemberships.isEmpty()) {
            log.warn("User already has an active membership: {}", activeMemberships.getFirst());
            return activeMemberships.getFirst()
                                    .toResponse();
        }

        var periodResult = new PeriodResult();
        var now = Instant.now();
        var membershipPeriodUnitString = portalConfigurationService.getEnumConfiguration(PortalConfigEnum.MEMBERSHIP.group, MEMBERSHIP_PERIOD_UNIT.key);
        var membershipPeriodUnit = ChronoUnit.valueOf(membershipPeriodUnitString);
        var membershipPeriodLength = portalConfigurationService.getNumericConfiguration(PortalConfigEnum.MEMBERSHIP.group, MEMBERSHIP_PERIOD_START_POINT.key);

        if (membershipType.equals(PERIODICAL)) {
            var membershipPeriodStartPoint = portalConfigurationService.getNumericConfiguration(PortalConfigEnum.MEMBERSHIP.group,
                    MEMBERSHIP_PERIOD_START_POINT.key);
            var calculationStart = portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key);
            var calculationStartDate = LocalDate.parse(calculationStart);
            periodResult = PeriodTool.calculatePeriod(now, calculationStartDate, membershipPeriodUnit, membershipPeriodStartPoint, membershipPeriodLength);
        } else {
            periodResult.setStartDate(LocalDate.now());
            periodResult.setEndDate(LocalDate.now()
                                             .plus(membershipPeriodLength, membershipPeriodUnit));
        }

        Membership membership = Membership.builder()
                                          .userId(membershipRequest.getUserId())
                                          .type(membershipType)
                                          .status(MembershipStatusEnum.ACTIVE)
                                          .startDate(LocalDate.now())
                                          .endDate(periodResult.getEndDate())
                                          .build();
        var newMembership = membershipRepository.save(membership);
        // The saved object does not have the user populated, so we fetch it
        var optionalUser = userService.findUserById(newMembership.getUserId());

        if (optionalUser.isEmpty()) {
            log.error("Could not find user for id: {}", newMembership.getUserId());
            return MembershipResponse.builder()
                                     .build();
        }

        var user = optionalUser.get();

        newMembership.setUser(user);
        return newMembership.toResponse();
    }

    @Transactional
    public MembershipResponse updateMembership(MembershipRequest membershipRequest) {
        var membershipType = getMembershipTypeSetting();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return MembershipResponse.builder()
                                     .build();
        }

        var optionalMembership = membershipRepository.findById(membershipRequest.getId());

        if (optionalMembership.isEmpty()) {
            log.error("Could not find membership for id: {}", membershipRequest.getId());
            return MembershipResponse.builder()
                                     .build();
        }

        var membership = optionalMembership.get();

        // We only allow updating the type from non-configured to the configured type
        if (!membership.getType()
                        .equals(membershipType)) {
            log.error("Membership type cannot be updated");
            return membership.toResponse();
        }

        membership.setType(membershipRequest.getType());

        membership.setStatus(membershipRequest.getStatus());
        // If the new status is not active, we set the end date to now
        if (!membershipRequest.getStatus()
                            .equals(MembershipStatusEnum.ACTIVE)) {
            membership.setEndDate(LocalDate.now());
        }

        var newMembership = membershipRepository.save(membership);

        return newMembership.toResponse();
    }

    private MembershipTypeEnum getMembershipTypeSetting() {
        var membershipTypeString = portalConfigurationService.getEnumConfiguration(PortalConfigEnum.MEMBERSHIP.group, MEMBERSHIP_TYPE.key);
        return MembershipTypeEnum.fromString(membershipTypeString);
    }
}
