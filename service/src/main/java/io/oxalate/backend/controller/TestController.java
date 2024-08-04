package io.oxalate.backend.controller;

import io.oxalate.backend.api.EventStatusEnum;
import static io.oxalate.backend.api.PaymentTypeEnum.ONE_TIME;
import static io.oxalate.backend.api.PaymentTypeEnum.PERIOD;
import io.oxalate.backend.api.RoleEnum;
import static io.oxalate.backend.api.UserStatus.ACTIVE;
import io.oxalate.backend.api.request.CertificateRequest;
import io.oxalate.backend.model.Event;
import io.oxalate.backend.model.Payment;
import io.oxalate.backend.model.RandomNameResponse;
import io.oxalate.backend.model.Role;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.PaymentRepository;
import io.oxalate.backend.rest.TestAPI;
import io.oxalate.backend.service.AuthService;
import io.oxalate.backend.service.CertificateService;
import io.oxalate.backend.service.EventService;
import io.oxalate.backend.service.RoleService;
import io.oxalate.backend.service.UserService;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Profile("local")
@Slf4j
@RequiredArgsConstructor
@RestController
public class TestController implements TestAPI {

    private final UserService userService;
    private final AuthService authService;
    private final RoleService roleService;
    private final EventService eventService;
    private final CertificateService certificateService;
    private static final String PASSWORD = "aA1^WWWWWWWWW";
    // We also use the repository in order to circumvent some business logic as well as temporal assumptions in the service
    private final PaymentRepository paymentRepository;

    private static final long DAYS_BACK = 2000L;
    private final EntityManager entityManager;
    private final List<RandomUserInfo> userData = new ArrayList<>();
    @Value("${oxalate.payment.period-start-month}")
    private int periodStartMonth;

    @Override
    public ResponseEntity<Void> generateRandomUsers(int numberOfUsers) {
        var randoUser = fetchRandomUserName(numberOfUsers);
        var randoCounter = 0;

        for (int i = 0; i < numberOfUsers; i++) {
            var user = createRandomUser(randoUser, i, randoCounter);
            randoCounter += 2;

            user = setRolesForRandomUser(user);
            generateRandomCertificatesForUser(user, Instant.now());
        }

        return null;
    }

    @Override
    public ResponseEntity<Void> generateYearsAgo(int yearsAgo) {
        var pinDate = Instant.now().minus(yearsAgo * 365L, ChronoUnit.DAYS);
        var users = 0L;

        // Prime the user data list with 50 users
        replenishUserData(50);

        while (pinDate.isBefore(Instant.now())) {
            // While there are less than 5 registered users, we don't generate an event nor forward the pin
            if (users < 5) {
                generateRandomUser(userData.remove(0), pinDate);
                users++;
                continue;
            }

            var fate = RandomUtils.nextInt(1, 11);

            if (fate < 6) {
                // New user
                generateRandomUser(userData.remove(0), pinDate);
                users++;
            } else if (fate < 9) {
                // New event
                randomEvent(pinDate);
            } else {
                // New payment
                randomPayment(pinDate);
            }

            // If the userData is emptied, then replenish it
            if (userData.isEmpty()) {
                replenishUserData(50);
            }

            // Finally we move the pin forward 1-7 days
            pinDate = pinDate.plus(RandomUtils.nextInt(1, 5), ChronoUnit.DAYS);
        }

        log.info("Generated {} years worth of events and users", yearsAgo);
        return ResponseEntity.ok().build();
    }

    private void generateRandomCertificatesForUser(User user, Instant createDate) {
        // Generate 1-7 certificates for user, everyone has at least one,  1 in 600 has 7
        var certificateCount = RandomUtils.nextInt(1, 601);
        if (certificateCount % 600 == 0) {
            certificateCount = 7;
        }

        if (certificateCount % 99 == 0) {
            certificateCount = 6;
        }

        if (certificateCount % 41 == 0) {
            certificateCount = 5;
        }

        if (certificateCount % 19 == 0) {
            certificateCount = 4;
        }

        if (certificateCount % 13 == 0) {
            certificateCount = 3;
        }

        if (certificateCount % 7 == 0) {
            certificateCount = 2;
        }

        if (certificateCount > 7) {
            certificateCount = 1;
        }

        for (int j = 0; j < certificateCount; j++) {
            var certificateRequest = CertificateRequest.builder()
                    .organization(getCertificateOrganization())
                    .certificateName(getDiveCertificationPrefix() + " " + getDiveCertificationName())
                    .certificateId(RandomStringUtils.randomAlphabetic(7))
                    .diverId(RandomStringUtils.randomAlphabetic(7))
                    .certificationDate(Timestamp.from(createDate.minus(RandomUtils.nextLong(100, 5000), ChronoUnit.DAYS)))
                    .build();
            while (certificateService.findCertificateByUserOrgAndCertification(user.getId(), certificateRequest.getOrganization(),
                    certificateRequest.getCertificateName()) != null) {
                certificateRequest = CertificateRequest.builder()
                        .organization(getCertificateOrganization())
                        .certificateName(getDiveCertificationPrefix() + " " + getDiveCertificationName())
                        .certificateId(RandomStringUtils.randomAlphabetic(7))
                        .diverId(RandomStringUtils.randomAlphabetic(7))
                        .certificationDate(Timestamp.from(createDate.minus(RandomUtils.nextLong(100, 5000), ChronoUnit.DAYS)))
                        .build();
            }

            certificateService.addCertificate(user.getId(), certificateRequest);
        }
    }

    private User setRolesForRandomUser(User user) {
        var roles = new HashSet<Role>();

        var userRole = roleService.findByName(RoleEnum.ROLE_USER.name());
        roles.add(userRole);
        // The first user is always an organizer
        if (user.getId() % 80 == 0 || user.getId() == 1) {
            var organizerRole = roleService.findByName(RoleEnum.ROLE_ORGANIZER.name());
            roles.add(organizerRole);
        }

        // The first user is always an administrator
        if (Math.log10(Math.toIntExact(user.getId()) * 10) % 1 == 0 || user.getId() == 1) {
            var adminRole = roleService.findByName(RoleEnum.ROLE_ADMIN.name());
            roles.add(adminRole);
        }

        user.setRoles(roles);
        return userService.updateUser(user);
    }

    private User createRandomUser(List<String> randoUser, int i, int randoCounter) {
        var firstName = randoUser.get(randoCounter++);
        var lastName = randoUser.get(randoCounter);
        var username = firstName + "." + lastName + "_" + i + "@test.tld";
        log.info("Generating user: " + username);
        var password = authService.generatePasswordHash(PASSWORD);
        var phoneNumber = String.valueOf(RandomUtils.nextLong(30000000000L, 99999999999L));
        var nextOfKin = RandomStringUtils.randomAlphabetic(18) + " " + RandomUtils.nextLong(30000000000L, 99999999999L);

        var user = User.builder()
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .status(ACTIVE)
                .privacy(false)
                .password(password)
                .nextOfKin(nextOfKin)
                .phoneNumber(phoneNumber)
                .registered(Instant.now().minus(RandomUtils.nextLong(0, DAYS_BACK), ChronoUnit.DAYS))
                .approvedTerms(true)
                .build();

        return userService.save(user);
    }

    private void randomEvent(Instant createInstant) {

        var organizerId = getRandomOrganizerId();
        var eventSize = RandomUtils.nextInt(3, 24);

        var event = Event.builder()
                .title("Event " + RandomStringUtils.randomAlphabetic(10))
                .description("Event " + RandomStringUtils.randomAlphabetic(100))
                .eventDuration(RandomUtils.nextInt(4, 24))
                .maxDuration(RandomUtils.nextInt(30, 240))
                .maxDepth(RandomUtils.nextInt(30, 180))
                .maxParticipants(eventSize)
                .startTime(Timestamp.from(createInstant))
                .organizerId(organizerId)
                .status(EventStatusEnum.PUBLISHED)
                .type(getEventType())
                .build();
        var newEvent = eventService.save(event);

        populateEventWithUsers(newEvent);
    }

    private List<String> fetchRandomUserName(long numberOfUsers) {
        var userInfos = new ArrayList<String>();
        Mono<RandomNameResponse> randomResponseMono = WebClient
                .create()
                .get()
                .uri("https://randomuser.me/api/?results=" + numberOfUsers)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(RandomNameResponse.class)
                .log();

        var randomNameResponse = randomResponseMono.block();

        for (RandomNameResponse.Result result : randomNameResponse.getResults()) {
            userInfos.add(result.getName().getFirst().replace(" ", "_"));
            userInfos.add(result.getName().getLast().replace(" ", "_"));
        }

        return userInfos;
    }

    private void randomPayment(Instant createInstant) {
        // Get user ID which has no active payment
        var queryString = "SELECT u.id "
                + "FROM users u "
                + "WHERE u.id NOT IN "
                + "    (SELECT DISTINCT p.user_id "
                + "     FROM payments p"
                + "     WHERE "
                + "           (p.expires_at > :currentTime AND p.payment_type = 'PERIOD')"
                + "        OR (p.payment_type = 'ONE_TIME' AND p.payment_count > 0))";

        var query = entityManager.createNativeQuery(queryString);
        query.setParameter("currentTime", Timestamp.from(createInstant));
        List results = query.getResultList();

        if (results.isEmpty()) {
            return;
        }

        var userId = (Long) results.get(RandomUtils.nextInt(0, results.size()));

        var optionalUser = userService.findUserById(userId);
        if (optionalUser.isEmpty()) {
            log.error("User with ID {} not found when generating payment", userId);
            return;
        }

        generateRandomPaymentForUser(optionalUser.get(), createInstant);
    }

    private void replenishUserData(int numberOfNames) {
        Mono<RandomNameResponse> randomResponseMono = WebClient
                .create()
                .get()
                .uri("https://randomuser.me/api/?results=" + numberOfNames)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(RandomNameResponse.class)
                .log();

        var randomNameResponse = randomResponseMono.block();
        assert randomNameResponse != null;

        for (RandomNameResponse.Result result : randomNameResponse.getResults()) {
            userData.add(new RandomUserInfo(result.getName().getFirst(), result.getName().getLast(), result.getEmail()));
        }
    }

    private User generateRandomUser(RandomUserInfo userInfo, Instant createInstant) {
        var password = authService.generatePasswordHash(PASSWORD);
        var phoneNumber = String.valueOf(RandomUtils.nextLong(30000000000L, 99999999999L));
        var nextOfKin = "Next" + RandomStringUtils.randomAlphabetic(18) + " " + RandomUtils.nextLong(30000000000L, 99999999999L);
        var language = List.of("de", "fi", "en", "sv")
                           .get(RandomUtils.nextInt(0, 4));
        // Make sure the username is unique
        while (true) {
            var user = userService.findByUsername(userInfo.getEmail());

            if (user.isEmpty()) {
                break;
            }

            userInfo.setEmail(RandomStringUtils.randomAlphabetic(4) + "_" + userInfo.getEmail());
        }

        var user = User.builder()
                .username(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .status(ACTIVE)
                .privacy(RandomUtils.nextInt(1, 13) < 2)
                .password(password)
                .nextOfKin(nextOfKin)
                       .language(language)
                .phoneNumber(phoneNumber)
                .registered(createInstant)
                .approvedTerms(RandomUtils.nextInt(1, 13) > 2)
                .build();

        var newUser = userService.save(user);
        newUser = setRolesForRandomUser(newUser);

        generateRandomCertificatesForUser(newUser, createInstant);
        // When a user registers, they always also make an payment, either period or one time
        generateRandomPaymentForUser(newUser, createInstant);
        return userService.save(newUser);
    }

    private void generateRandomPaymentForUser(User user, Instant createInstant) {
        Payment payment;

        if (RandomUtils.nextInt(1, 9) < 6) {
            payment = Payment.builder()
                    .paymentType(ONE_TIME)
                    .userId(user.getId())
                    .createdAt(createInstant)
                    .paymentCount(RandomUtils.nextInt(1, 6))
                    .build();
        } else {
            var localDate = createInstant.atZone(ZoneOffset.UTC).toLocalDate();
            var currentMonth = localDate.getMonthValue();
            var endYear = localDate.getYear();

            if (currentMonth >= periodStartMonth) {
                endYear++;
            }

            var endDate = Instant.parse(endYear + "-" + String.format("%02d", periodStartMonth) + "-01T00:00:00.00Z");

            payment = Payment.builder()
                    .paymentType(PERIOD)
                    .userId(user.getId())
                    .createdAt(createInstant)
                    .expiresAt(endDate)
                    .build();
        }
        paymentRepository.save(payment);
    }

    private void populateEventWithUsers(Event event) {
        var participantAmount = RandomUtils.nextInt(3, event.getMaxParticipants());

        var userIdList = new ArrayList<Long>();
        // If the event is surface only, then we pick any active user
        if (event.getType().equals("Vain pintatoimintaa")) {
            userService.findAll().forEach(user -> userIdList.add(user.getId()));
        } else {
            // Get user ID which has an active payment
            var queryString = "SELECT u.id "
                    + "FROM users u "
                    + "WHERE u.id IN "
                    + "    (SELECT DISTINCT p.user_id "
                    + "     FROM payments p"
                    + "     WHERE "
                    + "           (p.expires_at > :currentTime AND p.payment_type = 'PERIOD')"
                    + "        OR (p.payment_type = 'ONE_TIME' AND p.payment_count > 0))";

            var query = entityManager.createNativeQuery(queryString);
            query.setParameter("currentTime", event.getStartTime());
            List results = query.getResultList();

            for (Object result : results) {
                var resultLong = (Long) result;
                userIdList.add(resultLong);
            }
        }

        // Remove the organizer from the list
        userIdList.remove(event.getOrganizerId());

        // If we have less active users than participants allotted, then we cut down the amount of participants
        if (userIdList.size() < participantAmount) {
            participantAmount = userIdList.size() - 1;
        }

        for (int j = 0; j < participantAmount; j++) {
            var participantId = userIdList.remove(RandomUtils.nextInt(0, userIdList.size()));

            // If the event is surface-only then instead of deducting payment, we add a one-time payment for the user
            if (event.getType().equals("Vain pintatoimintaa")) {
                addEventPaymentFromUser(participantId);
            } else {
                deductEventPaymentFromUser(participantId, event.getStartTime().toInstant());
            }

            var optionalParticipant = userService.findUserById(participantId);
            assert optionalParticipant.isPresent();
            var participant = optionalParticipant.get();

            eventService.addUserToEvent(participant, event.getId());
            var diveCount = RandomUtils.nextInt(1, 1000);

            if (diveCount % 499 == 0) {
                diveCount = 3;
            }

            if (diveCount % 99 == 0) {
                diveCount = 2;
            }

            if (diveCount > 3) {
                diveCount = 1;
            }

            if (event.getType().equals("Vain pintatoimintaa")) {
                diveCount = 0;
            }

            eventService.updateUserDiveCount(event.getId(), participantId, diveCount);
        }
    }

    private void addEventPaymentFromUser(Long participantId) {
        var oneTimePayment = paymentRepository.findByUserIdAndAndPaymentType(participantId, ONE_TIME.name());

        if (oneTimePayment.isEmpty() || oneTimePayment.get().getPaymentCount() < 1) {
            var payment = Payment.builder()
                    .userId(participantId)
                    .paymentType(ONE_TIME)
                    .createdAt(Instant.now())
                    .paymentCount(0)
                    .build();
            oneTimePayment = Optional.of(paymentRepository.save(payment));
        }

        var payment = oneTimePayment.get();
        payment.setPaymentCount(payment.getPaymentCount() + 1);
        paymentRepository.save(payment);
    }

    private void deductEventPaymentFromUser(long participantId, Instant eventStartTime) {
        // Check first if the user has an active period payment, if it is present, then we 'use' it, otherwise we try to use a one-time payment
        var optionalPeriodPayment = paymentRepository.findByUserIdAndAndPaymentType(participantId, PERIOD.name());

        if (optionalPeriodPayment.isPresent()) {
            return;
        }

        var optionalPayment = paymentRepository.findByUserIdAndAndPaymentType(participantId, ONE_TIME.name());

        if (optionalPayment.isPresent()) {
            var payment = optionalPayment.get();
            payment.setPaymentCount(payment.getPaymentCount() - 1);
            paymentRepository.save(payment);
        }
    }

    private String getDiveCertificationName() {
        return switch (RandomUtils.nextInt(0, 36)) {
            case 0 -> "Drysuit diver";
            case 1 -> "Elite drysuit diver";
            case 2 -> "OWD";
            case 3 -> "AOWD";
            case 4 -> "OC nitrox";
            case 5 -> "OC normoxic";
            case 6 -> "OC trimix";
            case 7 -> "CCR nitrox";
            case 8 -> "CCR normoxic";
            case 9 -> "CCR trimix";
            case 10 -> "Cavern";
            case 11 -> "OC Mine I";
            case 12 -> "OC Mine II";
            case 13 -> "OC Intro to cave";
            case 14 -> "OC Full cave";
            case 15 -> "OC Expedition cave";
            case 16 -> "CCR Mine I";
            case 17 -> "CCR Mine II";
            case 18 -> "CCR Intro to cave";
            case 19 -> "CCR Full cave";
            case 20 -> "CCR Expedition cave";
            case 21 -> "Cave DPV";
            case 22 -> "Dive master";
            case 23 -> "Rescue diver";
            case 24 -> "Open Water Side Mount Diver";
            case 25 -> "Rescue Diver";
            case 26 -> "EANx Diver";
            case 27 -> "Deep Diver";
            case 28 -> "EANx Diver";
            case 29 -> "Speciality Diver";
            case 30 -> "Diver";
            case 31 -> "Public Safety";
            case 32 -> "Open Water DPV";
            case 33 -> "Decompression Specialist";
            case 34 -> "Self-sufficient Diver";
            case 35 -> "Diver First Aid";
            default -> "Bubble maker";
        };
    }

    private String getEventType() {
        return switch (RandomUtils.nextInt(0, 6)) {
            case 0 -> "Sukellus";
            case 1 -> "Luola";
            case 2 -> "Luola / Avo";
            case 3 -> "Avo";
            case 4 -> "Vain pintatoimintaa";
            default -> "Virtasukellus";
        };
    }

    private String getDiveCertificationPrefix() {
        return switch (RandomUtils.nextInt(0, 6)) {
            case 0 -> "Beginner";
            case 1 -> "Intermediate";
            case 2 -> "Advanced";
            case 3 -> "Experienced";
            case 4 -> "Professional";
            case 5 -> "Elite";
            default -> "Stroke";
        };
    }

    private String getCertificateOrganization() {
        return switch (RandomUtils.nextInt(0, 23)) {
            case 0 -> "NAUI";
            case 1 -> "PADI";
            case 2 -> "RAID";
            case 3 -> "SSI";
            case 4 -> "BSAC";
            case 5 -> "TDI";
            case 6 -> "CMAS";
            case 7 -> "SDI";
            case 8 -> "GUE";
            case 9 -> "ACUC";
            case 10 -> "PTRD";
            case 11 -> "FFESSM";
            case 12 -> "IDA";
            case 13 -> "IAC";
            case 14 -> "IAHD";
            case 15 -> "WOSD";
            case 16 -> "NASE";
            case 17 -> "EUF";
            case 18 -> "ANMP";
            case 19 -> "ANDI";
            case 20 -> "PSS";
            case 21 -> "USOA";
            case 22 -> "NSS/CDS";
            default -> "IANTD";
        };
    }

    @Builder
    @Data
    private static class RandomUserInfo {
        private String firstName;
        private String lastName;
        private String email;
    }

    private long getRandomOrganizerId() {
        var organizers = userService.findAllByRole(RoleEnum.ROLE_ORGANIZER);

        if (organizers.isEmpty()) {
            return 1L;
        }

        return organizers.get(RandomUtils.nextInt(0, organizers.size())).getId();
    }
}
