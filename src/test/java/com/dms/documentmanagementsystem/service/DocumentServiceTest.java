package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.exception.NotFoundException;
import com.dms.documentmanagementsystem.messaging.DocumentUploadedEvent;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.model.DocumentReview;
import com.dms.documentmanagementsystem.model.ReviewStatus;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import com.dms.documentmanagementsystem.repository.DocumentReviewRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    @Test
    void create_savesDocument_andPublishesEvent() {
        S3Client s3 = Mockito.mock(S3Client.class);
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentReviewRepository reviewRepo = Mockito.mock(DocumentReviewRepository.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);

        DocumentService service = new DocumentService(s3, repo, reviewRepo, rabbitTemplate);

        ReflectionTestUtils.setField(service, "exchange", "dms.exchange");
        ReflectionTestUtils.setField(service, "uploadRoutingKey", "dms.document.uploaded");

        Document toSave = new Document();
        toSave.setFileName("a.pdf");

        Document saved = new Document();
        saved.setId(1L);
        saved.setFileName("a.pdf");

        when(repo.save(any(Document.class))).thenReturn(saved);
        when(reviewRepo.findByDocumentId(anyLong())).thenReturn(Optional.empty());
        when(reviewRepo.save(any(DocumentReview.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = service.create(toSave);

        assertEquals(1L, result.getId());
        verify(repo).save(any(Document.class));
        verify(reviewRepo).save(any(DocumentReview.class));
        verify(rabbitTemplate).convertAndSend(
                eq("dms.exchange"),
                eq("dms.document.uploaded"),
                any(DocumentUploadedEvent.class)
        );
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        S3Client s3 = Mockito.mock(S3Client.class);
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentReviewRepository reviewRepo = Mockito.mock(DocumentReviewRepository.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        DocumentService service = new DocumentService(s3, repo, reviewRepo, rabbitTemplate);

        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void update_updatesFields() {
        S3Client s3 = Mockito.mock(S3Client.class);
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentReviewRepository reviewRepo = Mockito.mock(DocumentReviewRepository.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        DocumentService service = new DocumentService(s3, repo, reviewRepo, rabbitTemplate);

        Document existing = new Document();
        existing.setId(5L);
        existing.setFileName("old.pdf");
        existing.setSummary("old");

        when(repo.findById(5L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document changes = new Document();
        changes.setFileName("new.pdf");
        changes.setSummary("new");

        Document result = service.update(5L, changes);

        assertEquals("new.pdf", result.getFileName());
        assertEquals("new", result.getSummary());
        verify(repo).save(any(Document.class));
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        S3Client s3 = Mockito.mock(S3Client.class);
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentReviewRepository reviewRepo = Mockito.mock(DocumentReviewRepository.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        DocumentService service = new DocumentService(s3, repo, reviewRepo, rabbitTemplate);

        when(repo.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(42L));
        verify(repo, never()).deleteById(anyLong());
    }
}
