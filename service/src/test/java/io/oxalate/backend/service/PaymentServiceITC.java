package io.oxalate.backend.service;

import io.oxalate.backend.AbstractIntegrationTest;
import io.oxalate.backend.api.PaymentTypeEnum;
import io.oxalate.backend.api.request.PaymentRequest;
import io.oxalate.backend.repository.PaymentRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentServiceITC extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldSaveAndRetrievePayment() {
        // Given
        var paymentRequest = PaymentRequest.builder()
                                           .userId(1L)
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
}
