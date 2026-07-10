package io.oxalate.backend.service;

import io.oxalate.backend.api.NotificationGroupEnum;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.repository.UserRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationGroupResolverServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private NotificationGroupResolverService service;

    private List<Long> testUserIds;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        testUserIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        testUsers = testUserIds.stream()
                               .map(id -> {
                                   User user = new User();
                                   user.setId(id);
                                   user.setFirstName("First " + id);
                                   user.setLastName("Last " + id);
                                   return user;
                               })
                               .collect(Collectors.toList());
    }

    @Test
    void testResolveGroupUsersWithAllRegistered() {
        when(userRepository.findAllActiveUserIds()).thenReturn(testUserIds);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.ALL_REGISTERED, null);

        assertEquals(testUserIds, result);
        verify(userRepository).findAllActiveUserIds();
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void testResolveGroupUsersWithInactiveDays() {
        List<User> inactiveUsers = Arrays.asList(testUsers.get(0), testUsers.get(2), testUsers.get(4));
        List<Long> inactiveUserIds = inactiveUsers.stream()
                                                  .map(User::getId)
                                                  .collect(Collectors.toList());

        when(userRepository.findUsersInactiveSince(any(Instant.class))).thenReturn(inactiveUsers);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.INACTIVE_DAYS, 7);

        assertEquals(inactiveUserIds, result);
        verify(userRepository).findUsersInactiveSince(any(Instant.class));
    }

    @Test
    void testResolveGroupUsersWithActiveMembership() {
        List<Long> activeMemberUserIds = Arrays.asList(2L, 4L);

        when(membershipRepository.findUserIdsWithActiveMembership()).thenReturn(activeMemberUserIds);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.ACTIVE_MEMBERSHIP, null);

        assertEquals(activeMemberUserIds, result);
        verify(membershipRepository).findUserIdsWithActiveMembership();
    }

    @Test
    void testResolveGroupUsersWithNoActiveMembership() {
        List<Long> activeMemberUserIds = Arrays.asList(2L, 4L);
        List<Long> expectedResult = Arrays.asList(1L, 3L, 5L);

        when(userRepository.findAllActiveUserIds()).thenReturn(testUserIds);
        when(membershipRepository.findUserIdsWithActiveMembership()).thenReturn(activeMemberUserIds);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.NO_ACTIVE_MEMBERSHIP, null);

        assertEquals(expectedResult.size(), result.size());
        assertTrue(result.containsAll(expectedResult));
        assertTrue(expectedResult.containsAll(result));
        verify(userRepository).findAllActiveUserIds();
        verify(membershipRepository).findUserIdsWithActiveMembership();
    }

    @Test
    void testResolveGroupUsersWithLockedAccounts() {
        List<User> lockedUsers = Arrays.asList(testUsers.get(0), testUsers.get(4));
        List<Long> lockedUserIds = lockedUsers.stream()
                                              .map(User::getId)
                                              .collect(Collectors.toList());

        when(userRepository.findLockedUsers()).thenReturn(lockedUsers);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.LOCKED_ACCOUNTS, null);

        assertEquals(lockedUserIds, result);
        verify(userRepository).findLockedUsers();
    }

    @Test
    void testResolveGroupUsersWithNeverHadMembership() {
        List<Long> neverHadMembershipUserIds = Arrays.asList(1L, 3L);

        when(membershipRepository.findUserIdsWhoNeverHadMembership()).thenReturn(neverHadMembershipUserIds);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.NEVER_HAD_MEMBERSHIP, null);

        assertEquals(neverHadMembershipUserIds, result);
        verify(membershipRepository).findUserIdsWhoNeverHadMembership();
    }

    @Test
    void testResolveGroupUsersReturnsEmptyListWhenNoUsersFound() {
        when(userRepository.findAllActiveUserIds()).thenReturn(Collections.emptyList());

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.ALL_REGISTERED, null);

        assertTrue(result.isEmpty());
        verify(userRepository).findAllActiveUserIds();
    }

    @Test
    void testResolveGroupUsersWithInactiveDaysAndNull() {
        List<Long> inactiveUserIds = Arrays.asList(1L, 2L);

        when(userRepository.findUsersInactiveSince(any(Instant.class))).thenReturn(Collections.emptyList());

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.INACTIVE_DAYS, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveGroupUsersInactiveDaysMultipleDays() {
        List<User> inactiveUsers = Arrays.asList(testUsers.get(0), testUsers.get(1), testUsers.get(2));
        List<Long> inactiveUserIds = inactiveUsers.stream()
                                                  .map(User::getId)
                                                  .collect(Collectors.toList());

        when(userRepository.findUsersInactiveSince(any(Instant.class))).thenReturn(inactiveUsers);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.INACTIVE_DAYS, 30);

        assertEquals(inactiveUserIds, result);
        verify(userRepository).findUsersInactiveSince(any(Instant.class));
    }

    @Test
    void testResolveGroupUsersNoActiveMembershipExcludesAllMembers() {
        List<Long> activeMemberUserIds = testUserIds; // Everyone has membership

        when(userRepository.findAllActiveUserIds()).thenReturn(testUserIds);
        when(membershipRepository.findUserIdsWithActiveMembership()).thenReturn(activeMemberUserIds);

        List<Long> result = service.resolveGroupUsers(NotificationGroupEnum.NO_ACTIVE_MEMBERSHIP, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveGroupUsersNullGroup() {
        assertThrows(Exception.class, () -> {
            service.resolveGroupUsers(null, null);
        });
    }
}
