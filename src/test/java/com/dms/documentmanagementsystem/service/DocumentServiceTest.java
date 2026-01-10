package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.exception.NotFoundException;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import com.dms.documentmanagementsystem.repository.DocumentReviewRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    @Test
    void getById_notFound_throwsNotFoundException() {
        S3Client s3 = Mockito.mock(S3Client.class);
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentReviewRepository reviewRepo = Mockito.mock(DocumentReviewRepository.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        ElasticsearchClient es = Mockito.mock(ElasticsearchClient.class);

        DocumentService service = new DocumentService(s3, repo, reviewRepo, rabbitTemplate, es);

        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(999L));
    }

    @Test
    void update_updatesFields() {
        S3Client s3 = Mockito.mock(S3Client.class);
        DocumentRepository repo = Mockito.mock(DocumentRepository.class);
        DocumentReviewRepository reviewRepo = Mockito.mock(DocumentReviewRepository.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        ElasticsearchClient es = Mockito.mock(ElasticsearchClient.class);

        DocumentService service = new DocumentService(s3, repo, reviewRepo, rabbitTemplate, es);

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
}
