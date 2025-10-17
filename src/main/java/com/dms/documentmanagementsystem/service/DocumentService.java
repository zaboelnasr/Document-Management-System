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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repo;
    private final RabbitTemplate rabbitTemplate;

    @Value("${dms.rmq.exchange}")
    private String exchange;

    @Value("${dms.rmq.routing.upload}")
    private String uploadRoutingKey;

    public DocumentService(DocumentRepository repo, RabbitTemplate rabbitTemplate) {
        this.repo = repo;
        this.rabbitTemplate = rabbitTemplate;
    }

    // ✅ NEW: handle file upload
    public Document handleFileUpload(MultipartFile file, String summary) {
        try {
            // 1️⃣ Create uploads directory if not exists
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 2️⃣ Clean file name and save file
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 3️⃣ Save document metadata in DB
            Document document = new Document();
            document.setFileName(fileName);
            document.setSummary(summary);
            document.setCreatedAt(LocalDateTime.now());

            Document saved = repo.save(document);
            log.info("Uploaded and saved document: id={}, fileName={}", saved.getId(), saved.getFileName());

            // 4️⃣ Publish RabbitMQ event for OCR
            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    saved.getId(), saved.getFileName(), saved.getSummary(), saved.getCreatedAt());
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

            // Publish event to RabbitMQ
            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    saved.getId(), saved.getFileName(), saved.getSummary(), saved.getCreatedAt());
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
        if (!repo.existsById(id)) {
            throw new NotFoundException("Document " + id + " not found");
        }
        repo.deleteById(id);
        log.info("Document deleted: id={}", id);
    }

    public Document getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }

    public Page<Document> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }
}
