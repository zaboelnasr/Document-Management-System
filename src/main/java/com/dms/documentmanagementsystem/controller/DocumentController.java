package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping
    public Document upload(@RequestBody Document doc) {
        return service.saveDocument(doc);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> get(@PathVariable Long id) {
        return service.getDocument(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Document> all() {
        return service.getAllDocuments();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteDocument(id);
    }
}
