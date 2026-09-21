package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.PageFile;
import io.oxalate.backend.repository.PageRoleAccessRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.PageFileRepository;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PageFileTransferServiceUTC {

    @Mock
    private PageFileRepository pageFileRepository;
    @Mock
    private PageRoleAccessRepository pageRoleAccessRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private PageFileTransferService pageFileTransferService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pageFileTransferService, "backendUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(pageFileTransferService, "uploadMainDirectory", "/tmp");
    }

    @Test
    void findAllPageFilesPagedMapsSortAndUrlOk() {
        var pageFile = PageFile.builder()
                               .id(6L)
                               .fileName("logo.png")
                               .mimeType("image/png")
                               .fileSize(12L)
                               .fileChecksum("abc")
                               .createdAt(Instant.now())
                               .status(UploadStatusEnum.PUBLISHED)
                               .language("en")
                               .pageId(3L)
                               .creator(User.builder()
                                            .id(2L)
                                            .firstName("Ann")
                                            .lastName("Diver")
                                            .build())
                               .build();
        when(pageFileRepository.findAll(ArgumentMatchers.<Specification<PageFile>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pageFile)));

        var page = pageFileTransferService.findAllPageFilesPaged(PagedRequest.builder()
                                                                             .sortBy("page_id")
                                                                             .direction(Sort.Direction.ASC)
                                                                             .search("logo")
                                                                             .build());

        assertEquals(1, page.getTotalElements());
        assertEquals("http://localhost:8080/api/files/page-files/3/en/logo.png", page.getContent()
                                                                                    .getFirst()
                                                                                    .getUrl());
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(pageFileRepository).findAll(ArgumentMatchers.<Specification<PageFile>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("pageId");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void findAllPageFilesPagedUnknownSortFallsBackOk() {
        when(pageFileRepository.findAll(ArgumentMatchers.<Specification<PageFile>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        pageFileTransferService.findAllPageFilesPaged(PagedRequest.builder()
                                                                  .sortBy("creator.password")
                                                                  .build());

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(pageFileRepository).findAll(ArgumentMatchers.<Specification<PageFile>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }
}
