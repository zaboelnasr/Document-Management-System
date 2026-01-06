package com.dms.documentmanagementsystem.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dms.documentmanagementsystem.exception.NotFoundException;
import com.dms.documentmanagementsystem.messaging.DocumentUploadedEvent;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentService service;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Test
    void getById_notFound_throwsNotFoundException() {
        S3Client s3 = mock(S3Client.class);
        DocumentRepository repo = mock(DocumentRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        service = new DocumentService(s3, repo, rabbitTemplate, elasticsearchClient);

        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void update_updatesFields() {
        S3Client s3 = mock(S3Client.class);
        DocumentRepository repo = mock(DocumentRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        service = new DocumentService(s3, repo, rabbitTemplate, elasticsearchClient);

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
    }

    @Test
    void delete_nonExisting_doesNothing() {
        S3Client s3 = mock(S3Client.class);
        DocumentRepository repo = mock(DocumentRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        service = new DocumentService(s3, repo, rabbitTemplate, elasticsearchClient);

        when(repo.findById(42L)).thenReturn(Optional.empty());

        // should NOT throw
        assertDoesNotThrow(() -> service.delete(42L));

        verify(repo, never()).delete(any());
    }
}
