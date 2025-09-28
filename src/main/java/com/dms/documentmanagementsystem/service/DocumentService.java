package com.dms.documentmanagementsystem.service;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class DocumentService {
    private final DocumentRepository repo;

    public DocumentService(DocumentRepository repo) {
        this.repo = repo;
    }

    public Document create(Document doc) {
        return repo.save(doc);
    }

    public Document update(Long id, Document changes) {
        Document existing = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document " + id + " not found"));
        existing.setFileName(changes.getFileName());
        existing.setSummary(changes.getSummary());
        return repo.save(existing);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NoSuchElementException("Document " + id + " not found");
        }
        repo.deleteById(id);
    }

    public Document getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document " + id + " not found"));
    }

    public Page<Document> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }
}
