package io.oxalate.backend.service;

import static io.oxalate.backend.api.MembershipStatusEnum.ACTIVE;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIODICAL;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.repository.EventParticipantsRepository;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventEligibilityUTC {
    private static final Instant EVENT_TIME = Instant.parse("2028-09-23T09:00:00Z");

    @Mock
    private PortalConfigurationService portalConfigurationService;
    @Mock
    private EventParticipantsRepository eventParticipantsRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void paymentMustBeValidAtEventTime() {
        when(paymentRepository.findAllByUserIdOrderByStartDateDesc(7L)).thenReturn(List.of(Payment.builder()
                                                                                                  .userId(7L)
                                                                                                  .paymentType(PERIODICAL)
                                                                                                  .paymentCount(0)
                                                                                                  .startDate(LocalDate.of(2026, 1, 1))
                                                                                                  .endDate(LocalDate.of(2027, 1, 1))
                                                                                                  .build()));

        assertTrue(paymentService.getBestAvailablePaymentTypeAtDate(7L, Instant.parse("2027-01-01T09:00:00Z"))
                                 .isEmpty());
    }

    @Test
    void missingPaymentIsRejected() {
        when(paymentRepository.findAllByUserIdOrderByStartDateDesc(7L)).thenReturn(List.of());

        assertTrue(paymentService.getBestAvailablePaymentTypeAtDate(7L, EVENT_TIME)
                                 .isEmpty());
    }

    @Test
    void paymentValidAtEventTimeIsAccepted() {
        when(paymentRepository.findAllByUserIdOrderByStartDateDesc(7L)).thenReturn(List.of(Payment.builder()
                                                                                                  .userId(7L)
                                                                                                  .paymentType(PERIODICAL)
                                                                                                  .paymentCount(0)
                                                                                                  .startDate(LocalDate.of(2028, 1, 1))
                                                                                                  .endDate(LocalDate.of(2029, 1, 1))
                                                                                                  .build()));

        assertEquals(PERIODICAL, paymentService.getBestAvailablePaymentTypeAtDate(7L, EVENT_TIME)
                                               .orElseThrow());
    }

    @Test
    void membershipMustBeValidAtEventTime() {
        when(membershipRepository.findByUserId(7L)).thenReturn(List.of(Membership.builder()
                                                                                 .userId(7L)
                                                                                 .status(ACTIVE)
                                                                                 .startDate(LocalDate.of(2026, 1, 1))
                                                                                 .endDate(LocalDate.of(2027, 1, 1))
                                                                                 .build()));

        assertFalse(membershipService.hasActiveMembershipAtDate(7L, EVENT_TIME));
    }

    @Test
    void missingMembershipIsRejected() {
        when(membershipRepository.findByUserId(7L)).thenReturn(List.of());

        assertFalse(membershipService.hasActiveMembershipAtDate(7L, EVENT_TIME));
    }
}
