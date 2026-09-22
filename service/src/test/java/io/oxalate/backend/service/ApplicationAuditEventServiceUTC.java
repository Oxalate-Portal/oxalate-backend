package io.oxalate.backend.service;

import io.oxalate.backend.api.AuditLevelEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.ApplicationAuditEvent;
import io.oxalate.backend.model.User;
import io.oxalate.backend.repository.ApplicationAuditEventRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ApplicationAuditEventServiceUTC {

    @Mock
    private ApplicationAuditEventRepository applicationAuditEventRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private ApplicationAuditEventService applicationAuditEventService;

    @Test
    void getAuditEventsPagedMapsAliasSortColumnsOk() {
        stubFindAll(auditEvent(1L, 7L));
        when(userService.findUserEntityById(7L)).thenReturn(User.builder()
                                                              .id(7L)
                                                              .firstName("Ann")
                                                              .lastName("Diver")
                                                              .build());

        var page = applicationAuditEventService.getAuditEventsPaged(PagedRequest.builder()
                                                                                .page(2)
                                                                                .size(50)
                                                                                .sortBy("address")
                                                                                .direction(Sort.Direction.ASC)
                                                                                .build(), null);

        var pageable = capturePageable();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        var order = pageable.getSort()
                            .getOrderFor("ipAddress");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
        assertEquals("Diver Ann", page.getContent()
                                      .getFirst()
                                      .getUserName());
    }

    @Test
    void getAuditEventsPagedUnknownSortFallsBackToCreatedAtDescOk() {
        stubFindAll();

        applicationAuditEventService.getAuditEventsPaged(PagedRequest.builder()
                                                                     .sortBy("password")
                                                                     .build(), "message");

        var order = capturePageable().getSort()
                                     .getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void getAuditEventsPagedUserNameFilterResolvesUserIdsOk() {
        stubFindAll();
        when(userService.findUsersByName("Ann")).thenReturn(List.of(User.builder()
                                                                       .id(7L)
                                                                       .build()));

        applicationAuditEventService.getAuditEventsPaged(PagedRequest.builder()
                                                                     .search("Ann")
                                                                     .build(), "user_name");

        verify(userService).findUsersByName("Ann");
        captureSpecification();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuditEventsPagedUserNameFilterWithoutMatchesMatchesNothingOk() {
        stubFindAll();
        when(userService.findUsersByName("Nobody")).thenReturn(List.of());
        var root = (Root<ApplicationAuditEvent>) mock(Root.class);
        var query = (CriteriaQuery<?>) mock(CriteriaQuery.class);
        var criteriaBuilder = mock(CriteriaBuilder.class);
        var nothing = mock(Predicate.class);
        when(criteriaBuilder.disjunction()).thenReturn(nothing);

        applicationAuditEventService.getAuditEventsPaged(PagedRequest.builder()
                                                                     .search("Nobody")
                                                                     .build(), "user_name");

        var specification = captureSpecification();
        assertEquals(nothing, specification.toPredicate(root, query, criteriaBuilder));
        verify(root, never()).get(ArgumentMatchers.anyString());
    }

    @Test
    void getAuditEventsPagedUnknownFilterColumnIsIgnoredOk() {
        stubFindAll();

        applicationAuditEventService.getAuditEventsPaged(PagedRequest.builder()
                                                                     .search("anything")
                                                                     .build(), "userId");

        verify(userService, never()).findUsersByName(ArgumentMatchers.anyString());
        captureSpecification();
    }

    @Test
    void getAuditEventsForUserPagedMarksUnknownUsersOk() {
        stubFindAll(auditEvent(1L, 9L), auditEvent(2L, null));
        when(userService.findUserEntityById(9L)).thenReturn(null);

        var page = applicationAuditEventService.getAuditEventsForUserPaged(9L, PagedRequest.builder()
                                                                                          .sortBy("user_name")
                                                                                          .build());

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent()
                       .stream()
                       .allMatch(entry -> "Unknown".equals(entry.getUserName())));
        assertNotNull(capturePageable().getSort()
                                       .getOrderFor("userId"));
    }

    private void stubFindAll(ApplicationAuditEvent... auditEvents) {
        when(applicationAuditEventRepository.findAll(ArgumentMatchers.<Specification<ApplicationAuditEvent>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditEvents)));
    }

    private Pageable capturePageable() {
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(applicationAuditEventRepository).findAll(ArgumentMatchers.<Specification<ApplicationAuditEvent>>any(), pageableCaptor.capture());
        return pageableCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Specification<ApplicationAuditEvent> captureSpecification() {
        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(applicationAuditEventRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
        var specification = (Specification<ApplicationAuditEvent>) specificationCaptor.getValue();
        assertNotNull(specification);
        return specification;
    }

    private static ApplicationAuditEvent auditEvent(long id, Long userId) {
        return ApplicationAuditEvent.builder()
                                    .id(id)
                                    .userId(userId)
                                    .level(AuditLevelEnum.INFO)
                                    .traceId("trace-" + id)
                                    .ipAddress("127.0.0.1")
                                    .source("UTC")
                                    .message("message " + id)
                                    .createdAt(Instant.now())
                                    .build();
    }
}
