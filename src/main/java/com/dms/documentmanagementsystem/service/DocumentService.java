package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.dto.DocumentSearchResultDTO;
import com.dms.documentmanagementsystem.exception.NotFoundException;
import com.dms.documentmanagementsystem.exception.ServiceException;
import com.dms.documentmanagementsystem.messaging.DocumentUploadedEvent;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final S3Client s3;
    private final DocumentRepository repo;
    private final RabbitTemplate rabbitTemplate;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${dms.rmq.exchange}")
    private String exchange;

    @Value("${dms.rmq.routing.upload}")
    private String uploadRoutingKey;

    // ----------------------------------------------------
    // Upload + index (Option B — CORRECT)
    // ----------------------------------------------------
    public Document handleFileUpload(MultipartFile file, String summary) {
        try {
            String cleanName = file.getOriginalFilename();
            if (cleanName == null || cleanName.isBlank()) {
                cleanName = "upload.pdf";
            }

            String objectKey = UUID.randomUUID() + "-" + cleanName;

            // 1️⃣ Upload to S3 / MinIO
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            // 2️⃣ Save DB
            Document doc = new Document();
            doc.setFileName(cleanName);
            doc.setSummary(summary);
            doc.setBucket(bucket);
            doc.setObjectKey(objectKey);
            doc.setContentType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setCreatedAt(LocalDateTime.now());

            Document saved = repo.save(doc);
            log.info("Uploaded and saved document id={}", saved.getId());

            // 3️⃣ INDEX IMMEDIATELY ✅
            indexDocument(saved);

            // 4️⃣ Publish event (OCR etc.)
            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    saved.getId(),
                    saved.getFileName(),
                    saved.getSummary(),
                    saved.getCreatedAt(),
                    saved.getBucket(),
                    saved.getObjectKey()
            );

            rabbitTemplate.convertAndSend(exchange, uploadRoutingKey, event);
            log.info("Published DocumentUploadedEvent for id={}", saved.getId());

            return saved;

        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new ServiceException("File upload failed", e);
        }
    }

    // ----------------------------------------------------
    // Elasticsearch indexing (SINGLE SOURCE OF TRUTH)
    // ----------------------------------------------------
    private void indexDocument(Document doc) {
        try {
            String contentText = doc.getContent() != null
                    ? new String(doc.getContent(), StandardCharsets.UTF_8)
                    : "";

            DocumentSearchResultDTO esDoc =
                    new DocumentSearchResultDTO(
                            doc.getId(),
                            doc.getFileName(),
                            contentText,
                            doc.getSummary()
                    );

            elasticsearchClient.index(i -> i
                    .index("documents")
                    .id(doc.getId().toString())
                    .document(esDoc)
            );

            log.info("Indexed document id={} into Elasticsearch", doc.getId());

        } catch (Exception e) {
            log.error("Failed to index document id={}", doc.getId(), e);
        }
    }

    // ----------------------------------------------------
    // CRUD
    // ----------------------------------------------------
    public Page<Document> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public Document getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }

    public Document update(Long id, Document changes) {
        Document existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));

        existing.setFileName(changes.getFileName());
        existing.setSummary(changes.getSummary());

        Document updated = repo.save(existing);
        indexDocument(updated); // keep ES in sync
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Optional<Document> opt = repo.findById(id);
        if (opt.isEmpty()) return;

        Document doc = opt.get();

        // delete S3
        if (doc.getBucket() != null && doc.getObjectKey() != null) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(doc.getBucket())
                        .key(doc.getObjectKey())
                        .build());
            } catch (Exception e) {
                log.warn("S3 delete failed for id={}", id);
            }
        }

        try {
            repo.delete(doc);
            log.info("Deleted document id={}", id);
        } catch (ObjectOptimisticLockingFailureException ignored) {
        }
    }

    public Document updateSummary(Long id, String summary) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));

        if (summary != null && summary.length() > 2000) {
            summary = summary.substring(0, 2000);
        }

        doc.setSummary(summary);
        Document updated = repo.save(doc);

        indexDocument(updated); // keep ES updated
        return updated;
    }
}
