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
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.api.response.MembershipResponse;
import io.oxalate.backend.api.response.PagedResponse;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.PeriodResult;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.tools.PagingTools;
import io.oxalate.backend.tools.PeriodTools;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MembershipService {

    private static final String MEMBERSHIP_DISABLED_WARNING = "Membership creation is disabled";
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "id", "id",
            "user_id", "userId",
            "username", "user.lastName",
            "status", "status",
            "type", "type",
            "start_date", "startDate",
            "end_date", "endDate",
            "created", "created");
    private static final String DEFAULT_SORT_COLUMN = "userId";

    private final MembershipRepository membershipRepository;
    private final PortalConfigurationService portalConfigurationService;
    private final UserService userService;

    /**
     * One page of the memberships that are active today or in the future. When memberships are disabled by
     * configuration the page is empty. The search matches the member's first and last name.
     *
     * @param pagedRequest paging, sorting and search parameters
     * @return the requested page
     */
    public PagedResponse<MembershipResponse> getAllActiveMembershipsPaged(PagedRequest pagedRequest) {
        var membershipType = getMembershipTypeSetting();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return PagingTools.emptyPage(pagedRequest);
        }

        var pageable = PagingTools.toPageable(pagedRequest, SORTABLE_COLUMNS, DEFAULT_SORT_COLUMN, Sort.Direction.ASC);
        var specification = PagingTools.allOf(currentAndFutureActive(LocalDate.now()),
                PagingTools.searchSpecification(pagedRequest, "user.firstName", "user.lastName"),
                PagingTools.enumSearchSpecification(pagedRequest, MembershipStatusEnum.class, "status"),
                PagingTools.enumSearchSpecification(pagedRequest, MembershipTypeEnum.class, "type"));

        return PagedResponse.fromPage(membershipRepository.findAll(specification, pageable), Membership::toResponse);
    }

    /**
     * Status ACTIVE and an end date of today or later, or no end date at all.
     */
    private static Specification<Membership> currentAndFutureActive(LocalDate today) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("status"), MembershipStatusEnum.ACTIVE),
                criteriaBuilder.or(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), today),
                        criteriaBuilder.isNull(root.get("endDate"))));
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

    public boolean hasActiveMembershipAtDate(long userId, Instant eventTime) {
        var eventDate = eventTime.atZone(java.time.ZoneId.systemDefault())
                                 .toLocalDate();
        return membershipRepository.findByUserId(userId)
                                   .stream()
                                   .anyMatch(membership -> membership.getStatus() == MembershipStatusEnum.ACTIVE
                                           && !membership.getStartDate()
                                                         .isAfter(eventDate)
                                           && (membership.getEndDate() == null || !membership.getEndDate()
                                                                                             .isBefore(eventDate)));
    }

    @Transactional
    public MembershipResponse createMembership(MembershipRequest membershipRequest) {
        var membershipType = getMembershipTypeSetting();
        var periodResult = getPeriodResult(membershipType, LocalDate.now());
        var requestedStartDate = membershipRequest.getStartDate() != null ? membershipRequest.getStartDate() : periodResult.getStartDate();
        var requestedEndDate = membershipRequest.getEndDate() != null ? membershipRequest.getEndDate() : periodResult.getEndDate();

        if (membershipType.equals(MembershipTypeEnum.DISABLED)) {
            log.warn(MEMBERSHIP_DISABLED_WARNING);
            return MembershipResponse.builder()
                                     .build();
        }

        // Check that the user does not already have an overlapping membership, only one active membership is allowed at a time
        var activeMemberships = membershipRepository.findByUserId(membershipRequest.getUserId())
                                                    .stream()
                                                    .filter(membership -> membership.getStatus()
                                                                                    .equals(MembershipStatusEnum.ACTIVE)
                                                            && membership.getEndDate()
                                                                         .isAfter(requestedStartDate))
                                                    .toList();

        if (!activeMemberships.isEmpty()) {
            log.warn("User already has an active membership: {}", activeMemberships.getFirst());
            return activeMemberships.getFirst()
                                    .toResponse();
        }

        var membership = Membership.builder()
                                   .userId(membershipRequest.getUserId())
                                   .type(membershipType)
                                   .status(MembershipStatusEnum.ACTIVE)
                                   .startDate(requestedStartDate)
                                   .endDate(requestedEndDate)
                                   .created(Instant.now())
                                   .build();
        var newMembership = membershipRepository.save(membership);
        // The saved object does not have the user populated, so we fetch it
        var user = userService.findUserEntityById(newMembership.getUserId());

        if (user == null) {
            log.error("Could not find user for id: {}", newMembership.getUserId());
            return MembershipResponse.builder()
                                     .build();
        }

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

    private PeriodResult getPeriodResult(MembershipTypeEnum membershipType, LocalDate localDateNow) {
        var periodResult = new PeriodResult();
        var membershipPeriodUnitString = portalConfigurationService.getEnumConfiguration(PortalConfigEnum.MEMBERSHIP.group, MEMBERSHIP_PERIOD_UNIT.key);
        var membershipPeriodUnit = ChronoUnit.valueOf(membershipPeriodUnitString);
        var membershipPeriodLength = portalConfigurationService.getNumericConfiguration(PortalConfigEnum.MEMBERSHIP.group, MEMBERSHIP_PERIOD_START_POINT.key);

        if (membershipType.equals(PERIODICAL)) {
            var membershipPeriodStartPoint = portalConfigurationService.getNumericConfiguration(PortalConfigEnum.MEMBERSHIP.group,
                    MEMBERSHIP_PERIOD_START_POINT.key);
            var calculationStart = portalConfigurationService.getStringConfiguration(PAYMENT.group, PAYMENT_PERIOD_START.key);
            var calculationStartDate = LocalDate.parse(calculationStart);
            periodResult = PeriodTools.calculatePeriod(localDateNow, calculationStartDate, membershipPeriodUnit, membershipPeriodStartPoint,
                    membershipPeriodLength);
        } else {
            periodResult.setStartDate(localDateNow);
            periodResult.setEndDate(localDateNow.plus(membershipPeriodLength, membershipPeriodUnit));
        }
        return periodResult;
    }
}
