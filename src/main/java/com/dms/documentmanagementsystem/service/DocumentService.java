package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {
    private final DocumentRepository repo;

    public DocumentService(DocumentRepository repo) {
        this.repo = repo;
    }

    public Document saveDocument(Document doc) {
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return repo.save(doc);
    }

    public Optional<Document> getDocument(Long id) {
        return repo.findById(id);
    }

    public List<Document> getAllDocuments() {
        return repo.findAll();
    }

    public void deleteDocument(Long id) {
        repo.deleteById(id);
    }
}
