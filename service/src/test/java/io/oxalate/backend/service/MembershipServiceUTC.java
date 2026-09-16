package io.oxalate.backend.service;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.request.MembershipRequest;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipServiceUTC {

    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private PortalConfigurationService portalConfigurationService;
    @Mock
    private UserService userService;
    @InjectMocks
    private MembershipService membershipService;

    @Test
    void disabledMembershipsReturnEmptyOrBlankResponses() {
        when(portalConfigurationService.getEnumConfiguration(any(), any()))
                .thenAnswer(invocation -> "membership-type".equals(invocation.getArgument(1)) ? "disabled" : "DAYS");

        assertTrue(membershipService.getAllActiveMemberships()
                                    .isEmpty());
        assertTrue(membershipService.getMembershipsForUser(1L)
                                    .isEmpty());
        assertEquals(0L, membershipService.findById(1L)
                                          .getId());
        assertEquals(0L, membershipService.createMembership(MembershipRequest.builder()
                                                                             .userId(1L)
                                                                             .build())
                                          .getId());
        verify(membershipRepository, never()).findById(anyLong());
    }

    @Test
    void findByIdReturnsBlankResponseWhenMissing() {
        when(portalConfigurationService.getEnumConfiguration(any(), any()))
                .thenAnswer(invocation -> "membership-type".equals(invocation.getArgument(1)) ? "perpetual" : "DAYS");
        when(membershipRepository.findById(4L)).thenReturn(Optional.empty());

        assertEquals(0L, membershipService.findById(4L)
                                          .getId());
    }

    @Test
    void activeMembershipDateChecksStatusAndBounds() {
        when(membershipRepository.findByUserId(3L)).thenReturn(List.of(
                Membership.builder()
                          .status(MembershipStatusEnum.ACTIVE)
                          .startDate(LocalDate.of(2025, 1, 1))
                          .endDate(LocalDate.of(2025, 12, 31))
                          .build(),
                Membership.builder()
                          .status(MembershipStatusEnum.EXPIRED)
                          .startDate(LocalDate.of(2025, 1, 1))
                          .endDate(null)
                          .build()));

        assertTrue(membershipService.hasActiveMembershipAtDate(3L, Instant.parse("2025-06-01T00:00:00Z")));
        assertTrue(!membershipService.hasActiveMembershipAtDate(3L, Instant.parse("2026-06-01T00:00:00Z")));
    }

    @Test
    void createMembershipReturnsExistingOverlap() {
        when(portalConfigurationService.getEnumConfiguration(any(), any()))
                .thenAnswer(invocation -> "membership-type".equals(invocation.getArgument(1)) ? "perpetual" : "DAYS");
        when(portalConfigurationService.getNumericConfiguration(any(), any())).thenReturn(1L);
        when(membershipRepository.findByUserId(3L)).thenReturn(List.of(
                Membership.builder()
                          .id(8L)
                          .userId(3L)
                          .user(User.builder()
                                    .firstName("Test")
                                    .lastName("User")
                                    .build())
                          .status(MembershipStatusEnum.ACTIVE)
                          .startDate(LocalDate.now()
                                              .minusDays(10))
                          .endDate(LocalDate.now()
                                            .plusDays(10))
                          .build()));

        var result = membershipService.createMembership(MembershipRequest.builder()
                                                                         .userId(3L)
                                                                         .build());

        assertEquals(8L, result.getId());
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void updateMembershipReturnsBlankWhenMissing() {
        when(portalConfigurationService.getEnumConfiguration(any(), any())).thenReturn("perpetual");
        when(membershipRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(0L, membershipService.updateMembership(MembershipRequest.builder()
                                                                             .id(9L)
                                                                             .build())
                                          .getId());
    }
}
