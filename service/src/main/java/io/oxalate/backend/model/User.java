package io.oxalate.backend.model;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UserStatus;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.api.response.AdminUserResponse;
import io.oxalate.backend.api.response.EventUserResponse;
import io.oxalate.backend.api.response.MembershipResponse;
import io.oxalate.backend.api.response.PaymentResponse;
import io.oxalate.backend.api.response.UserResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Setter
@Builder
@Entity
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Email
    @Column(name = "username")
    private String username;

    @NotBlank
    @Size(max = 120)
    @Column(name = "password")
    private String password;

    @NotBlank
    @Size(max = 120)
    @Column(name = "first_name")
    private String firstName;

    @NotBlank
    @Size(max = 120)
    @Column(name = "last_name")
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @NotNull
    @Column(name = "phone_number")
    private String phoneNumber;

    @NotNull
    @Column(name = "privacy")
    private boolean privacy;

    @Column(name = "next_of_kin")
    private String nextOfKin;

    @NotNull
    @Column(name = "registered")
    private Instant registered;

    @NotNull
    @Column(name = "approved_terms")
    private boolean approvedTerms;

    @Size(min = 2, max = 2, message = "Language code is given with 2 characters as per ISO-639-1")
    @Column(name = "language")
    private String language;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Transient
    private Set<Role> roles;

    @Transient
    private List<Event> organizedEvents;

    @Transient
    private List<Event> participatedEvents;

    @Transient
    private long diveCount;

    @Transient
    private Set<Payment> payments;

    @Transient
    private List<Membership> membership;

    public User(SignupRequest signupRequest) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.username = signupRequest.getUsername();
        this.password = passwordEncoder.encode(signupRequest.getPassword());
        this.firstName = signupRequest.getFirstName();
        this.lastName = signupRequest.getLastName();
        this.phoneNumber = signupRequest.getPhoneNumber();
        this.privacy = signupRequest.isPrivacy();
        this.nextOfKin = signupRequest.getNextOfKin();
        this.roles = new HashSet<>();
        this.roles.add(new Role(RoleEnum.ROLE_USER));
        this.organizedEvents = new ArrayList<>();
        this.participatedEvents = new ArrayList<>();
        this.status = UserStatus.REGISTERED;
        this.registered = Instant.now();
        this.approvedTerms = signupRequest.isApprovedTerms();
        this.language = signupRequest.getLanguage();
        this.diveCount = 0L;
        this.lastSeen = Instant.now();
    }

    public EventUserResponse toEventUserResponse() {
        var paymentResponses = new HashSet<PaymentResponse>();

        if (this.payments != null) {
            for (Payment payment : this.getPayments()) {
                paymentResponses.add(payment.toPaymentResponse());
            }
        }

        return EventUserResponse.builder()
                                .id(this.id)
                                .name(this.lastName + " " + this.firstName)
                                .eventDiveCount(this.diveCount)
                                .createdAt(null)
                                .payments(paymentResponses)
                                .build();
    }

    public UserResponse toUserResponse() {
        var paymentResponses = new HashSet<PaymentResponse>();

        if (this.payments != null) {
            for (Payment payment : this.getPayments()) {
                paymentResponses.add(payment.toPaymentResponse());
            }
        }

        return UserResponse.builder()
                           .id(this.id)
                           .firstName(this.firstName)
                           .lastName(this.lastName)
                           .username(this.username)
                           .phoneNumber(this.phoneNumber)
                           .registered(this.registered)
                           .diveCount(this.diveCount)
                           .payments(paymentResponses)
                           .approvedTerms(this.approvedTerms)
                           .language(this.language)
                           .build();
    }

    public AdminUserResponse toAdminUserResponse() {
        var paymentResponses = new HashSet<PaymentResponse>();

        if (this.payments != null) {
            for (Payment payment : this.getPayments()) {
                paymentResponses.add(payment.toPaymentResponse());
            }
        }

        var roleSet = new HashSet<String>();

        for (Role role : this.getRoles()) {
            roleSet.add(role.getName()
                            .name());
        }

        var membershipResponses = new ArrayList<MembershipResponse>();

        for (Membership membership : this.getMembership()) {
            membershipResponses.add(membership.toResponse());
        }

        return AdminUserResponse.builder()
                                .id(this.id)
                                .firstName(this.firstName)
                                .lastName(this.lastName)
                                .username(this.username)
                                .status(this.getStatus())
                                .phoneNumber(this.phoneNumber)
                                .privacy(this.isPrivacy())
                                .nextOfKin(this.getNextOfKin())
                                .registered(this.registered)
                                .approvedTerms(this.approvedTerms)
                                .diveCount(this.diveCount)
                                .payments(paymentResponses)
                                .memberships(membershipResponses)
                                .roles(roleSet)
                                .language(this.language)
                                .lastSeen(this.lastSeen)
                                .build();
    }
}
