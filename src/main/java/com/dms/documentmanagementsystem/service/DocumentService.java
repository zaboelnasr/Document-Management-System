package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.dto.DocumentSearchResultDTO;
import com.dms.documentmanagementsystem.exception.NotFoundException;
import com.dms.documentmanagementsystem.exception.ServiceException;
import com.dms.documentmanagementsystem.messaging.DocumentUploadedEvent;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.model.DocumentReview;
import com.dms.documentmanagementsystem.model.OcrStatus;
import com.dms.documentmanagementsystem.model.ReviewStatus;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import com.dms.documentmanagementsystem.repository.DocumentReviewRepository;
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
    private final DocumentReviewRepository reviewRepo;
    private final RabbitTemplate rabbitTemplate;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${dms.rmq.exchange}")
    private String exchange;

    @Value("${dms.rmq.routing.upload}")
    private String uploadRoutingKey;

    // ----------------------------------------------------
    // Upload + index
    // ----------------------------------------------------
    public Document handleFileUpload(MultipartFile file, String summary) {
        try {
            String cleanName = file.getOriginalFilename();
            if (cleanName == null || cleanName.isBlank()) {
                cleanName = "upload.pdf";
            }

            String objectKey = UUID.randomUUID() + "-" + cleanName;

            // 1) Upload to S3 / MinIO
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            // 2) Save DB
            Document doc = new Document();
            doc.setFileName(cleanName);
            doc.setSummary(summary);
            doc.setBucket(bucket);
            doc.setObjectKey(objectKey);
            doc.setContentType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setCreatedAt(LocalDateTime.now());
            doc.setOcrStatus(OcrStatus.PENDING);

            Document saved = repo.save(doc);
            ensureReview(saved, ReviewStatus.OPEN);
            log.info("Uploaded and saved document id={}", saved.getId());

            // 3) Index immediately
            indexDocument(saved);

            // 4) Publish event (OCR etc.)
            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    saved.getId(),
                    saved.getFileName(),
                    saved.getSummary(),
                    saved.getCreatedAt(),
                    saved.getBucket(),
                    saved.getObjectKey(),
                    saved.getReview() != null && saved.getReview().getStatus() != null
                            ? saved.getReview().getStatus().name()
                            : null
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
    // Elasticsearch indexing
    // ----------------------------------------------------
    private void indexDocument(Document doc) {
        try {
            String contentText = doc.getContent() != null
                    ? new String(doc.getContent(), StandardCharsets.UTF_8)
                    : "";

            String reviewStatus = null;
            if (doc.getReview() != null && doc.getReview().getStatus() != null) {
                reviewStatus = doc.getReview().getStatus().name();
            }

            DocumentSearchResultDTO esDoc =
                    new DocumentSearchResultDTO(
                            doc.getId(),
                            doc.getFileName(),
                            contentText,
                            doc.getSummary(),
                            doc.getCreatedAt(),
                            reviewStatus
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

    public Page<Document> getAll(Pageable pageable, ReviewStatus status) {
        if (status == null) {
            return repo.findAll(pageable);
        }
        return repo.findByReviewStatus(status, pageable);
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

        try {
            elasticsearchClient.delete(d -> d.index("documents").id(id.toString()));
        } catch (Exception e) {
            log.warn("Elasticsearch delete failed for id={}", id);
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

    public void updateReviewStatus(Long id, ReviewStatus status) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));

        DocumentReview review = reviewRepo.findByDocumentId(id)
                .orElseGet(() -> {
                    DocumentReview r = new DocumentReview();
                    r.setDocument(doc);
                    return r;
                });

        review.setStatus(status);
        reviewRepo.save(review);

        indexDocument(doc);
    }

    public void updateOcrStatus(Long id, OcrStatus status) {
        Document doc = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
        doc.setOcrStatus(status);
        repo.save(doc);
    }


    private void ensureReview(Document doc, ReviewStatus status) {
        Optional<DocumentReview> existing = reviewRepo.findByDocumentId(doc.getId());
        if (existing.isPresent()) {
            return;
        }
        DocumentReview review = new DocumentReview();
        review.setDocument(doc);
        review.setStatus(status);
        reviewRepo.save(review);
    }
}
