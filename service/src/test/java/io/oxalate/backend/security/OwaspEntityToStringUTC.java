package io.oxalate.backend.security;

import io.oxalate.backend.api.MembershipStatusEnum;
import io.oxalate.backend.api.MembershipTypeEnum;
import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.model.Membership;
import io.oxalate.backend.model.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * OWASP A09:2025 - Security Logging and Alerting Failures, and A10:2025 - Mishandling of Exceptional Conditions.
 * <p>
 * Lombok generates {@code toString()} over every field, which turns any log statement that interpolates an entity into
 * two separate problems: the password hash and the member's personal data end up in the log file, and the bidirectional
 * {@code User} to {@code Membership} association recurses until the stack overflows.
 * <p>
 * Both were real: the test suite logged {@code "[FAILED toString()]"} because {@code User.toString()} threw a
 * {@link StackOverflowError}.
 */
@DisplayName("OWASP A09/A10: entities are safe to log")
class OwaspEntityToStringUTC {

    private static final String SECRET_HASH = "$2a$12$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ012345";
    private static final String PHONE_NUMBER = "+358401234567";
    private static final String NEXT_OF_KIN = "Kin Person, +358409876543";

    private static User buildUser() {
        return User.builder()
                   .id(42L)
                   .username("member@example.org")
                   .password(SECRET_HASH)
                   .firstName("Member")
                   .lastName("Person")
                   .status(UserStatusEnum.ACTIVE)
                   .phoneNumber(PHONE_NUMBER)
                   .nextOfKin(NEXT_OF_KIN)
                   .privacy(true)
                   .registered(Instant.now())
                   .approvedTerms(true)
                   .language("en")
                   .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                   .build();
    }

    private static Membership buildMembership(User user) {
        return Membership.builder()
                         .id(7L)
                         .userId(user.getId())
                         .type(MembershipTypeEnum.PERIODICAL)
                         .status(MembershipStatusEnum.ACTIVE)
                         .startDate(LocalDate.now())
                         .endDate(LocalDate.now()
                                           .plusYears(1))
                         .created(Instant.now())
                         .user(user)
                         .build();
    }

    @Test
    @DisplayName("The password hash is never part of the user representation that ends up in logs")
    void userToStringOmitsPasswordOk() {
        assertThat(buildUser().toString()).doesNotContain(SECRET_HASH)
                                          .doesNotContain("$2a$");
    }

    @Test
    @DisplayName("Directly identifying contact details are not written to logs")
    void userToStringOmitsContactDetailsOk() {
        var representation = buildUser().toString();

        assertThat(representation).doesNotContain(PHONE_NUMBER)
                                  .doesNotContain(NEXT_OF_KIN);
    }

    @Test
    @DisplayName("A membership does not recurse back into its user")
    void membershipToStringDoesNotRecurseOk() {
        var user = buildUser();
        var membership = buildMembership(user);
        user.setMembership(List.of(membership));

        assertThatCode(membership::toString).doesNotThrowAnyException();
        assertThatCode(user::toString).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A membership representation carries no user personal data either")
    void membershipToStringOmitsUserDetailsOk() {
        var representation = buildMembership(buildUser()).toString();

        assertThat(representation).doesNotContain(SECRET_HASH)
                                  .doesNotContain(PHONE_NUMBER)
                                  .contains("userId=42");
    }

    @Test
    @DisplayName("Passwords hashed outside the security configuration use the same cost factor")
    void signupHashUsesConfiguredCostFactorOk() {
        assertThat(WebSecurityConfig.BCRYPT_STRENGTH).isGreaterThanOrEqualTo(12);

        var hash = new BCryptPasswordEncoder(WebSecurityConfig.BCRYPT_STRENGTH).encode("SomePassword1!");

        assertThat(hash).startsWith("$2a$" + WebSecurityConfig.BCRYPT_STRENGTH + "$");
    }
}
