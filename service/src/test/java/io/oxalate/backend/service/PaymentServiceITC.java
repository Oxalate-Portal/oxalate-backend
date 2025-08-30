package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.PaymentTypeEnum;
import static io.oxalate.backend.api.PortalConfigEnum.PAYMENT;
import static io.oxalate.backend.api.PortalConfigEnum.PaymentConfigEnum.SINGLE_PAYMENT_ENABLED;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.RoleEnum.ROLE_USER;
import io.oxalate.backend.api.UserStatusEnum;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentServiceITC extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PortalConfigurationService portalConfigurationService;

    private User diver;

    @BeforeEach
    void setUp() {
        this.diver = generateUser(ACTIVE, ROLE_USER);
        // Add event which starts in 10 days

        portalConfigurationService.setRuntimeValue(PAYMENT.group, SINGLE_PAYMENT_ENABLED.key, "true");
        portalConfigurationService.reloadPortalConfigurations();
    }

    @Test
    void shouldSaveAndRetrievePayment() {
        // Given
        var paymentRequest = PaymentRequest.builder()
                                           .userId(diver.getId())
                                           .paymentCount(4)
                                           .paymentType(PaymentTypeEnum.ONE_TIME)
                                           .build();

        // When
        var savedPayment = paymentService.savePayment(paymentRequest);

        // Then
        assertNotNull(savedPayment);
        assertNotNull(savedPayment.getPayments());
        assertFalse(savedPayment.getPayments()
                                .isEmpty());
        assertEquals(1, savedPayment.getPayments()
                                    .size());
        var paymentResponse = savedPayment.getPayments()
                                  .iterator()
                                  .next();
        var retrievedPayment = paymentRepository.findById(paymentResponse.getId());
        assertTrue(retrievedPayment.isPresent());
    }

    private User generateUser(UserStatusEnum userStatusEnum, RoleEnum roleEnum) {
        var randomUsername = "test-" + Instant.now()
                                              .toEpochMilli() + "@test.tld";
        var user = User.builder()
                       .username(randomUsername)
                       .password("password")
                       .firstName("Max")
                       .lastName("Mustermann")
                       .status(userStatusEnum)
                       .phoneNumber("123456789")
                       .privacy(false)
                       .nextOfKin("Maxine Mustermann")
                       .registered(Instant.now()
                                          .minus(1000L, ChronoUnit.DAYS))
                       .approvedTerms(true)
                       .language("de")
                       .lastSeen(Instant.now()
                                        .minus(1, ChronoUnit.DAYS))
                       .build();

        var newUser = userRepository.save(user);
        var optionalRole = roleRepository.findByName(roleEnum);
        assertFalse(optionalRole.isEmpty());
        var role = optionalRole.get();
        roleRepository.addUserRole(newUser.getId(), role.getId());
        return newUser;
    }
}
