package io.oxalate.backend.service;

import io.oxalate.backend.api.UserStatusEnum;
import io.oxalate.backend.api.UserTypeEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.CertificateRepository;
import io.oxalate.backend.repository.EventRepository;
import io.oxalate.backend.repository.MembershipRepository;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.service.filetransfer.AvatarFileTransferService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserServiceUTC {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private AvatarFileTransferService avatarFileTransferService;
    @Mock
    private CertificateRepository certificateRepository;
    @InjectMocks
    private UserService userService;

    @Test
    void findAllPagedMapsAllowedSortAndPopulatesUsersOk() {
        var user = User.builder()
                       .id(3L)
                       .username("ann@example.tld")
                       .firstName("Ann")
                       .lastName("Diver")
                       .status(UserStatusEnum.ACTIVE)
                       .registered(Instant.now())
                       .language("en")
                       .primaryUserType(UserTypeEnum.SCUBA_DIVER)
                       .build();
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        when(roleService.findRolesForUser(3L)).thenReturn(Set.of());
        when(membershipRepository.findAllCurrentAndFutureActiveByUserId(3L)).thenReturn(List.of());

        var page = userService.findAllPaged(PagedRequest.builder()
                                                        .page(1)
                                                        .size(20)
                                                        .sortBy("last_name")
                                                        .direction(Sort.Direction.DESC)
                                                        .search("ann")
                                                        .build());

        assertEquals(1, page.getTotalElements());
        assertEquals("Diver", page.getContent()
                                  .getFirst()
                                  .getLastName());
        assertEquals(UserStatusEnum.ACTIVE, page.getContent()
                                                .getFirst()
                                                .getStatus());
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue()
                                      .getPageNumber());
        assertEquals(20, pageableCaptor.getValue()
                                       .getPageSize());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("lastName");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void findAllPagedUnknownSortFallsBackToIdAscOk() {
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        var page = userService.findAllPaged(PagedRequest.builder()
                                                        .sortBy("password")
                                                        .build());

        assertTrue(page.isEmpty());
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("id");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
        assertEquals(null, pageableCaptor.getValue()
                                         .getSort()
                                         .getOrderFor("password"));
    }

    @Test
    void findUsersByNameSupportsSingleAndReversedFullNames() {
        when(userRepository.findAllByFirstNameContainsIgnoreCaseOrLastNameContainsIgnoreCase("Ann", "Ann"))
                .thenReturn(List.of());
        assertTrue(userService.findUsersByName("Ann")
                              .isEmpty());

        when(userRepository.findByFirstNameContainsIgnoreCaseAndLastNameContainsIgnoreCase("Ann", "Smith"))
                .thenReturn(List.of());
        when(userRepository.findByFirstNameContainsIgnoreCaseAndLastNameContainsIgnoreCase("Smith", "Ann"))
                .thenReturn(List.of());
        assertTrue(userService.findUsersByName("Ann Smith")
                              .isEmpty());
    }

    @Test
    void emailValidationRejectsMalformedAndAcceptsValidAddresses() {
        assertFalse(userService.isEmailAddressFormatValid(null));
        assertFalse(userService.isEmailAddressFormatValid("bad%name@example.com"));
        assertFalse(userService.isEmailAddressFormatValid("not-an-email"));
        assertTrue(userService.isEmailAddressFormatValid("person@example.com"));
    }

    @Test
    void usernameAvailabilityRejectsInvalidAndExistingAddresses() {
        assertFalse(userService.isUsernameAvailableForUser("invalid", 1L));
        when(userRepository.findByUsername("taken@example.com")).thenReturn(Optional.of(User.builder()
                                                                                            .id(2L)
                                                                                            .build()));
        when(certificateRepository.findByUserId(2L)).thenReturn(List.of());
        assertFalse(userService.isUsernameAvailableForUser(" taken@example.com ", 1L));
        when(userRepository.findByUsername("free@example.com")).thenReturn(Optional.empty());
        assertTrue(userService.isUsernameAvailableForUser(" free@example.com ", 1L));
    }

    @Test
    void updateUsernameNormalizesAndReportsMissingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder()
                                                                     .id(1L)
                                                                     .build()));
        assertTrue(userService.updateUsername(1L, " New@Example.com "));
        verify(userRepository).save(any(User.class));

        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertFalse(userService.updateUsername(2L, "missing@example.com"));
    }

    @Test
    void deleteUserOnlyDeletesRegisteredUsers() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        userService.deleteUser(1L);
        verify(userRepository, never()).deleteById(1L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder()
                                                                     .id(2L)
                                                                     .status(UserStatusEnum.ACTIVE)
                                                                     .build()));
        userService.deleteUser(2L);
        verify(userRepository, never()).deleteById(2L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(User.builder()
                                                                     .id(3L)
                                                                     .status(UserStatusEnum.REGISTERED)
                                                                     .build()));
        userService.deleteUser(3L);
        verify(roleService).deleteUserRoles(3L);
        verify(userRepository).deleteById(3L);
    }

    @Test
    void updateStatusAndAnswersIgnoreMissingUsers() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        userService.updateStatus(1L, UserStatusEnum.ACTIVE);
        userService.setTermAnswer(1L, true);
        userService.setHealthCheckAnswer(1L, true);
        verify(userRepository, never()).save(any(User.class));
    }
}
