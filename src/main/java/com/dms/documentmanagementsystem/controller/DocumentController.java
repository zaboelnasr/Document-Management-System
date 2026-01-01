package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.dto.DocumentRequestDTO;
import com.dms.documentmanagementsystem.dto.DocumentResponseDTO;
import com.dms.documentmanagementsystem.mapper.DocumentMapper;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.service.DocumentService;
import com.dms.indexing.DocumentSearchResult;
import com.dms.indexing.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;
    private final SearchService searchService;
    public DocumentController(DocumentService service, SearchService searchService) {
        this.service = service;
        this.searchService = searchService;
    }

    // ----------------------------------------------------
    // GET /api/documents
    // ----------------------------------------------------
    @GetMapping
    public Page<DocumentResponseDTO> getAll(Pageable pageable) {
        return service.getAll(pageable).map(DocumentMapper::toDTO);
    }

    // ----------------------------------------------------
    // GET /api/documents/{id}
    // ----------------------------------------------------
    @GetMapping("/{id}")
    public DocumentResponseDTO getOne(@PathVariable Long id) {
        return DocumentMapper.toDTO(service.getById(id));
    }

    // ----------------------------------------------------
    // POST /api/documents
    // ----------------------------------------------------
    @PostMapping
    public ResponseEntity<DocumentResponseDTO> create(
            @Valid @RequestBody DocumentRequestDTO request) {

        Document created = service.create(DocumentMapper.toEntity(request));
        DocumentResponseDTO body = DocumentMapper.toDTO(created);
        URI location = URI.create("/api/documents/" + created.getId());
        return ResponseEntity.created(location).body(body);
    }

    // ----------------------------------------------------
    // PUT /api/documents/{id}
    // ----------------------------------------------------
    @PutMapping("/{id}")
    public DocumentResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequestDTO request) {

        Document updated = service.update(id, DocumentMapper.toEntity(request));
        return DocumentMapper.toDTO(updated);
    }

    // ----------------------------------------------------
    // DELETE /api/documents/{id}
    // ----------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------
    // POST /api/documents/upload
    // ----------------------------------------------------
    @PostMapping("/upload")
    public Document upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("summary") String summary) {

        return service.handleFileUpload(file, summary);
    }

    // ----------------------------------------------------
    // PATCH /api/documents/{id}/summary    für Sprint 5
    // ----------------------------------------------------
    @PostMapping("/{id}/summary")
    public ResponseEntity<Void> updateSummary(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String summary = body.get("summary");
        if (summary == null) {
            return ResponseEntity.badRequest().build();
        }

        service.updateSummary(id, summary);
        return ResponseEntity.noContent().build(); // 204
    }

    // PATCH /api/documents/{id}/summary   -- Sprint 6
    @GetMapping("/search")
    public ResponseEntity<List<DocumentSearchResult>> search(@RequestParam String term) {
        return ResponseEntity.ok(searchService.search(term));
    }

}