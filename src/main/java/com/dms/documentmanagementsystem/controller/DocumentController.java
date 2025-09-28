package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) {
        this.service = service;
    }

    /**
     * Save a Document
     *
     * @param doc the document to be saved
     * @return saved document with location
     */
    @PostMapping
    public ResponseEntity<Document> upload(@RequestBody Document doc) {
        var saved = service.saveDocument(doc);
        return ResponseEntity
                .created(URI.create("/api/documents/" + saved.getId()))
                .body(saved);
    }

    /**
     * search for a Document
     *
     * @param id the document id
     * @return found document
     *         or 404 Not Found if non-existent param
     */
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

    /**
     * delete a specific Document
     *
     * @param id the document id
     * @return 204 No Content if deleted & if Document never existed
     *         or 404 Not Found if non-numeric param
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
