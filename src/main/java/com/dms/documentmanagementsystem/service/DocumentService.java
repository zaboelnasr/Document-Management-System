package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.exception.NotFoundException;
import com.dms.documentmanagementsystem.exception.ServiceException;
import com.dms.documentmanagementsystem.messaging.DocumentUploadedEvent;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final S3Client s3;
    private final DocumentRepository repo;
    private final RabbitTemplate rabbitTemplate;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${dms.rmq.exchange}")
    private String exchange;

    @Value("${dms.rmq.routing.upload}")
    private String uploadRoutingKey;

    public DocumentService(S3Client s3, DocumentRepository repo, RabbitTemplate rabbitTemplate) {
        this.s3 = s3;
        this.repo = repo;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Document handleFileUpload(MultipartFile file, String summary) {
        try {
            String cleanName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
            if (cleanName == null || cleanName.isBlank()) cleanName = "upload.pdf";
            String objectKey = java.util.UUID.randomUUID() + "-" + cleanName;

            s3.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes())
            );

            Document toSave = new Document();
            toSave.setFileName(cleanName);
            toSave.setSummary(summary);
            toSave.setCreatedAt(LocalDateTime.now());
            toSave.setBucket(bucket);
            toSave.setObjectKey(objectKey);
            toSave.setContentType(file.getContentType());
            toSave.setSize(file.getSize());

            Document saved = repo.save(toSave);
            log.info("Uploaded and saved document: id={}, fileName={}", saved.getId(), saved.getFileName());

            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    saved.getId(), saved.getFileName(), saved.getSummary(), saved.getCreatedAt(), saved.getBucket(), saved.getObjectKey());
            rabbitTemplate.convertAndSend(exchange, uploadRoutingKey, event);
            log.info("Published DocumentUploadedEvent for id={} to exchange='{}'", saved.getId(), exchange);

            return saved;

        } catch (IOException ex) {
            log.error("Failed to upload file", ex);
            throw new ServiceException("File upload failed", ex);
        }
    }

    public Document create(Document doc) {
        try {
            Document saved = repo.save(doc);
            log.info("Document created: id={}, fileName={}", saved.getId(), saved.getFileName());

            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    saved.getId(), saved.getFileName(), saved.getSummary(), saved.getCreatedAt(), saved.getBucket(), saved.getObjectKey());
            rabbitTemplate.convertAndSend(exchange, uploadRoutingKey, event);
            log.info("Published DocumentUploadedEvent for id={} to exchange='{}' with key='{}'",
                    saved.getId(), exchange, uploadRoutingKey);

            return saved;
        } catch (Exception ex) {
            log.error("Failed to create document or publish event", ex);
            throw new ServiceException("Failed to create document", ex);
        }
    }

    public Document update(Long id, Document changes) {
        Document existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
        existing.setFileName(changes.getFileName());
        existing.setSummary(changes.getSummary());
        Document updated = repo.save(existing);
        log.info("Document updated: id={}", updated.getId());
        return updated;
    }

    public void delete(Long id) {
        // fetch the doc to get bucket + objectKey
        Document doc = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));

        // 1) delete the object from S3/MinIO
        if (doc.getBucket() != null && doc.getObjectKey() != null) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(doc.getBucket())
                        .key(doc.getObjectKey())
                        .build());
                log.info("Deleted S3 object bucket={}, key={}", doc.getBucket(), doc.getObjectKey());
            } catch (Exception e) {
                // choose one behavior:
                // a) fail the whole operation (strict consistency):
                // throw new ServiceException("Failed to delete S3 object", e);

                // b) log and continue (eventual cleanup acceptable in dev):
                log.warn("Failed to delete S3 object (bucket={}, key={}): {}",
                        doc.getBucket(), doc.getObjectKey(), e.toString());
            }
        } else {
            log.warn("Document {} has no bucket/objectKey, skipping S3 delete", id);
        }

        // 2) delete DB record
        repo.deleteById(id);
        log.info("Deleted document id={}", id);
    }

    public Document getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }

    public Document updateSummary(Long id, String summary) {
        Document existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));

        if (summary != null && summary.length() > 2000) {
            // damit die DB nicht explodiert
            String shortened = summary.substring(0, 2000);
            log.warn("Summary too long ({} chars), truncating to 2000 characters for document id={}",
                    summary.length(), id);
            existing.setSummary(shortened);
        } else {
            existing.setSummary(summary);
        }

        Document updated = repo.save(existing);
        log.info("Summary updated for document id={}", updated.getId());
        return updated;
    }



    public Page<Document> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }
}
