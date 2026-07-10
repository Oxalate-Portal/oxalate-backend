package io.oxalate.backend.service;

import io.oxalate.backend.api.NotificationGroupEnum;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for resolving user groups for targeted notification sending.
 * Handles different user group targeting strategies based on activity, membership, and account status.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationGroupResolverService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    /**
     * Resolves the target user IDs for a given notification group.
     *
     * @param group        The notification group to resolve
     * @param inactiveDays The number of days for inactivity check (only used for INACTIVE_DAYS group)
     * @return List of user IDs that match the group criteria
     */
    @Transactional(readOnly = true)
    public List<Long> resolveGroupUsers(NotificationGroupEnum group, Integer inactiveDays) {
        return switch (group) {
            case ALL_REGISTERED -> resolveAllRegisteredUsers();
            case INACTIVE_DAYS -> resolveInactiveUsers(inactiveDays);
            case ACTIVE_MEMBERSHIP -> resolveActiveMembers();
            case NO_ACTIVE_MEMBERSHIP -> resolveNoActiveMembers();
            case LOCKED_ACCOUNTS -> resolveLockedAccounts();
            case NEVER_HAD_MEMBERSHIP -> resolveNeverHadMembership();
        };
    }

    /**
     * Get all registered users.
     */
    private List<Long> resolveAllRegisteredUsers() {
        log.debug("Resolving all registered users");
        var userIds = userRepository.findAllActiveUserIds();
        log.info("Resolved {} all registered users", userIds.size());
        return userIds;
    }

    /**
     * Get users who have had no activity (no event participation, no login) for a specified number of days.
     * Activity is determined by checking the lastSeen timestamp.
     */
    private List<Long> resolveInactiveUsers(Integer inactiveDays) {
        if (inactiveDays == null || inactiveDays <= 0) {
            log.warn("Invalid inactiveDays value: {}", inactiveDays);
            return List.of();
        }

        log.debug("Resolving users inactive for {} days", inactiveDays);
        var cutoffTime = Instant.now()
                                .minusSeconds((long) inactiveDays * 24 * 60 * 60);
        var users = userRepository.findUsersInactiveSince(cutoffTime);
        var userIds = users.stream()
                           .map(u -> u.getId())
                           .toList();
        log.info("Resolved {} users inactive for {} days", userIds.size(), inactiveDays);
        return userIds;
    }

    /**
     * Get users who have an active membership.
     */
    private List<Long> resolveActiveMembers() {
        log.debug("Resolving users with active membership");
        var users = membershipRepository.findUserIdsWithActiveMembership();
        log.info("Resolved {} users with active membership", users.size());
        return users;
    }

    /**
     * Get users who do not have an active membership.
     */
    private List<Long> resolveNoActiveMembers() {
        log.debug("Resolving users without active membership");
        var allUsers = userRepository.findAllActiveUserIds();
        var activeMembers = membershipRepository.findUserIdsWithActiveMembership();
        var users = allUsers.stream()
                            .filter(id -> !activeMembers.contains(id))
                            .toList();
        log.info("Resolved {} users without active membership", users.size());
        return users;
    }

    /**
     * Get users who have locked accounts.
     */
    private List<Long> resolveLockedAccounts() {
        log.debug("Resolving users with locked accounts");
        var users = userRepository.findLockedUsers();
        var userIds = users.stream()
                           .map(u -> u.getId())
                           .toList();
        log.info("Resolved {} users with locked accounts", userIds.size());
        return userIds;
    }

    /**
     * Get registered users who have never had a membership (active or past).
     */
    private List<Long> resolveNeverHadMembership() {
        log.debug("Resolving registered users who never had membership");
        var users = membershipRepository.findUserIdsWhoNeverHadMembership();
        log.info("Resolved {} users who never had membership", users.size());
        return users;
    }
}
