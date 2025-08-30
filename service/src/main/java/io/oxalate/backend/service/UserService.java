package io.oxalate.backend.service;

import io.oxalate.backend.api.ParticipantTypeEnum;
import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.api.UserStatusEnum;
import static io.oxalate.backend.api.UserStatusEnum.ANONYMIZED;
import static io.oxalate.backend.api.UserStatusEnum.REGISTERED;
import io.oxalate.backend.api.request.SignupRequest;
import io.oxalate.backend.model.Role;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;
    // Note! We can't use EventService here because it would cause a circular dependency
    private final EventRepository eventRepository;
    private final PaymentService paymentService;
    private final MembershipRepository membershipRepository;

    private final String ANONYMIZED_STRING = "<Anonymized>";

    public List<User> findAll() {
        var users = userRepository.findAllByOrderByIdAsc();

        for (User user : users) {
            populateUser(user);
        }

        return users;
    }

    public Optional<User> findUserById(long userId) {
        var optionalUser = userRepository.findById(userId);

        optionalUser.ifPresent(this::populateUser);
        return optionalUser;
    }

    public Optional<User> findByUsername(String username) {
        var optionalUser = userRepository.findByUsername(username.toLowerCase());
        optionalUser.ifPresent(this::populateUser);
        return optionalUser;
    }

    @Transactional
    public User createNewUser(SignupRequest signupRequest) {
        User user = new User(signupRequest);
        var newUser = userRepository.save(user);
        roleRepository.addUserRole(newUser.getId(),  roleService.getRoleId(user.getRoles().iterator().next()));
        return newUser;
    }

    @Transactional()
    public User updateUser(User user) {
        roleRepository.removeUserRoles(user.getId());

        for (Role role : user.getRoles()) {
            // Get the role from the database
            var optionalRole = roleRepository.findByName(role.getName());
            if (optionalRole.isPresent()) {
                role = optionalRole.get();
            }
            roleRepository.addUserRole(user.getId(), role.getId());
        }

        var newUser = userRepository.save(user);
        populateUser(newUser);

        return newUser;
    }

    public List<User> findEventParticipants(Long eventId) {
        var participants = userRepository.findEventParticipantsByTypesOrderByRegistrationTime(eventId, List.of(ParticipantTypeEnum.USER.name()));

        for (User user : participants) {
            populateUser(user);
        }

        return participants;
    }

    private void populateUser(User user) {
        var roles = roleService.findRolesForUser(user.getId());
        user.setRoles(roles);

        var events = eventRepository.findByUserId(user.getId());
        user.setParticipatedEvents(events);

        var organizedEvents = eventRepository.findByOrganizer(user.getId());
        user.setOrganizedEvents(organizedEvents);

        var memberships = membershipRepository.findByUserId(user.getId());
        user.setMembership(memberships);
        user.setPayments(paymentService.getActivePaymentsByUser(user.getId()));
        user.setDiveCount(eventRepository.countDivesByUserId(user.getId()));
    }

    @Transactional
    public void logoutUser(long userId) {
        var optionalUser = userRepository.findById(userId);

        if (optionalUser.isPresent()) {
            var user = optionalUser.get();
            user.setLastSeen(Instant.now());
            userRepository.save(user);
        }
    }

    @Transactional
    public void anonymize(long userId) {
        var optionalUser = userRepository.findById(userId);

        if (optionalUser.isPresent()) {
            var user = optionalUser.get();

            user.setFirstName(ANONYMIZED_STRING);
            user.setLastName(ANONYMIZED_STRING);
            user.setPassword(ANONYMIZED_STRING);
            user.setStatus(ANONYMIZED);
            user.setPrivacy(false);
            user.setPhoneNumber(ANONYMIZED_STRING);
            user.setNextOfKin(ANONYMIZED_STRING);
            // The username must remain unique, so we generate a random one
            user.setUsername(UUID.randomUUID() + "@anonymized.tld");
            userRepository.save(user);
        }
    }

    public List<User> findAllByRole(RoleEnum roleEnum) {
        var userList = new ArrayList<User>();
        var role = roleRepository.findByName(roleEnum);

        if (role.isEmpty()) {
            return userList;
        }

        var users = userRepository.findAllByRole(role.get().getId());

        for (User user : users) {
            populateUser(user);
            userList.add(user);
        }

        return userList;
    }

    /**
     * Finds a user by their name(s). If the given string contains spaces, we will try all combinations of first and last names.
     *
     * @param name The name(s) to search for
     * @return List of users matching the given name(s)
     */

    public List<User> findUsersByName(String name) {
        var userList = new ArrayList<User>();

        if (name.contains(" ")) {
            var splittedNames = name.split(" ", 2);
            var firstName = splittedNames[0];
            var secondName = splittedNames[1];

            log.debug("Searching multiple names: {} {}", firstName, secondName);

            userList.addAll(userRepository.findByFirstNameContainsIgnoreCaseAndLastNameContainsIgnoreCase(firstName, secondName));
            userList.addAll(userRepository.findByFirstNameContainsIgnoreCaseAndLastNameContainsIgnoreCase(secondName, firstName));
        } else {
            log.debug("Searching single name {}", name);
            userList.addAll(userRepository.findAllByFirstNameContainsIgnoreCaseOrLastNameContainsIgnoreCase(name, name));
        }

        return userList;
    }

    /**
     * Deletes a user from the database. Note that this shall only be used for registered, but not active users
     * @param userId The ID of the user to delete
     */
    public void deleteUser(long userId) {
        var optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            log.warn("Attempted to delete a non-existing user with ID " + userId);
            return;
        }

        if (optionalUser.get().getStatus() != REGISTERED) {
            log.warn("Attempted to delete a user which is not in REGISTERED-status with ID " + userId);
            return;
        }

        roleService.deleteUserRoles(userId);
        userRepository.deleteById(userId);
    }

    public void updateStatus(long userId, UserStatusEnum userStatusEnum) {
        var optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            log.warn("Attempted to update status of a non-existing user with ID " + userId);
            return;
        }

        var user = optionalUser.get();
        user.setStatus(userStatusEnum);
        userRepository.save(user);
    }

    /**
     * Checks if a username is valid. A username is valid if it is not already taken and does not contain the % character
     * as we don't fully trust the @Email annotation checks. Besides the % character is a valid character in an email address which we don't want to allow.
     * @param username The username to check
     * @return True if the username is valid, false otherwise
     */
    public boolean isUsernameValid(String username) {
        return findByUsername(username).isEmpty() &&
                !username.contains("%");
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void setTermAnswer(long userId, boolean termAnswer) {
        var user = userRepository.findById(userId);

        if (user.isPresent()) {
            user.get().setApprovedTerms(termAnswer);
            userRepository.save(user.get());
        }
    }

    @Transactional
    public void resetTermAnswer() {
        userRepository.resetTermAnswer();
    }
}
