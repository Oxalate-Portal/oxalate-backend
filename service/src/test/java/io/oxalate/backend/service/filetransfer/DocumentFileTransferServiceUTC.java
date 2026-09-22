package io.oxalate.backend.service.filetransfer;

import io.oxalate.backend.api.UploadStatusEnum;
import io.oxalate.backend.api.request.PagedRequest;
import io.oxalate.backend.model.User;
import io.oxalate.backend.model.filetransfer.DocumentFile;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.repository.filetransfer.DocumentFileRepository;
import io.oxalate.backend.service.RoleService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentFileTransferServiceUTC {

    @Mock
    private DocumentFileRepository documentFileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @InjectMocks
    private DocumentFileTransferService documentFileTransferService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentFileTransferService, "backendUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(documentFileTransferService, "uploadMainDirectory", "/tmp");
    }

    @Test
    void findAllDocumentFilesPagedMapsSortAndUrlOk() {
        var documentFile = DocumentFile.builder()
                                       .id(4L)
                                       .fileName("rules.pdf")
                                       .mimeType("application/pdf")
                                       .fileSize(12L)
                                       .fileChecksum("abc")
                                       .createdAt(Instant.now())
                                       .status(UploadStatusEnum.PUBLISHED)
                                       .creator(User.builder()
                                                    .id(2L)
                                                    .firstName("Ann")
                                                    .lastName("Diver")
                                                    .build())
                                       .build();
        when(documentFileRepository.findAll(ArgumentMatchers.<Specification<DocumentFile>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(documentFile)));

        var page = documentFileTransferService.findAllDocumentFilesPaged(PagedRequest.builder()
                                                                                     .sortBy("mimetype")
                                                                                     .direction(Sort.Direction.ASC)
                                                                                     .build(), null);

        assertEquals("http://localhost:8080/api/files/documents/4", page.getContent()
                                                                        .getFirst()
                                                                        .getUrl());
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentFileRepository).findAll(ArgumentMatchers.<Specification<DocumentFile>>any(), pageableCaptor.capture());
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("mimeType");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllDocumentFilesPagedFiltersByCreatorOk() {
        when(documentFileRepository.findAll(ArgumentMatchers.<Specification<DocumentFile>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var root = (Root<DocumentFile>) mock(Root.class);
        var creatorPath = (Path<Object>) mock(Path.class);
        var idPath = (Path<Object>) mock(Path.class);
        var query = (CriteriaQuery<?>) mock(CriteriaQuery.class);
        var criteriaBuilder = mock(CriteriaBuilder.class);
        var equal = mock(Predicate.class);
        when(root.get("creator")).thenReturn(creatorPath);
        when(creatorPath.get("id")).thenReturn(idPath);
        when(criteriaBuilder.equal(idPath, 9L)).thenReturn(equal);

        documentFileTransferService.findAllDocumentFilesPaged(PagedRequest.builder()
                                                                          .sortBy("nope")
                                                                          .build(), 9L);

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentFileRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        var specification = (Specification<DocumentFile>) specificationCaptor.getValue();
        assertEquals(equal, specification.toPredicate(root, query, criteriaBuilder));
        var order = pageableCaptor.getValue()
                                  .getSort()
                                  .getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }
}
