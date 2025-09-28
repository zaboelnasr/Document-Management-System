package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    // ✅ Constructor injection
    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    @PostMapping
    public Document createDocument(@RequestBody Document document) {
        return documentRepository.save(document);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable Long id, @RequestBody Document updatedDoc) {
        return documentRepository.findById(id)
                .map(existingDoc -> {
                    existingDoc.setFileName(updatedDoc.getFileName());
                    existingDoc.setSummary(updatedDoc.getSummary());
                    return ResponseEntity.ok(documentRepository.save(existingDoc));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        if (!documentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
