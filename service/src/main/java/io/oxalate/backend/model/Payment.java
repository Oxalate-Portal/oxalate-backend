package io.oxalate.backend.model;

import io.oxalate.backend.api.PaymentTypeEnum;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.download.DownloadPaymentResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private PaymentTypeEnum paymentType;

    // Depending of the type we either use the counter, or the dates for expiration
    @Column(name = "payment_count")
    private int paymentCount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public PaymentResponse toPaymentResponse() {
        return switch (this.paymentType) {
            case ONE_TIME -> PaymentResponse.builder()
                                            .id(this.id)
                                            .userId(this.userId)
                                            .paymentType(this.paymentType)
                                            .paymentCount((long) this.paymentCount)
                                            .createdAt(this.createdAt)
                                            .build();
            case PERIOD -> PaymentResponse.builder()
                                          .id(this.id)
                                          .userId(this.userId)
                                          .paymentCount(null)
                                          .paymentType(this.paymentType)
                                          .createdAt(this.createdAt)
                                          .expiresAt(this.expiresAt)
                                          .build();
            default -> throw new IllegalArgumentException("Unknown payment type");
        };
    }

    public DownloadPaymentResponse toDownloadPaymentResponse() {
        return DownloadPaymentResponse.builder()
                                      .id(this.id)
                                      .userId(this.userId)
                                      .name(null)
                                      .paymentType(this.paymentType)
                                      .paymentCount(this.paymentType == PaymentTypeEnum.ONE_TIME ? this.paymentCount : null)
                                      .createdAt(this.createdAt)
                                      .build();
    }
}
