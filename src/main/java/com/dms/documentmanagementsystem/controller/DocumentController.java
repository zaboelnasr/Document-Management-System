package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.dto.DocumentRequestDTO;
import com.dms.documentmanagementsystem.dto.DocumentResponseDTO;
import com.dms.documentmanagementsystem.mapper.DocumentMapper;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public Page<DocumentResponseDTO> getAll(Pageable pageable) {
        return service.getAll(pageable).map(DocumentMapper::toDTO);
    }

    @GetMapping("/{id}")
    public DocumentResponseDTO getOne(@PathVariable Long id) {
        return DocumentMapper.toDTO(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<DocumentResponseDTO> create(@Valid @RequestBody DocumentRequestDTO request) {
        Document created = service.create(DocumentMapper.toEntity(request));
        DocumentResponseDTO body = DocumentMapper.toDTO(created);
        URI location = URI.create("/api/documents/" + created.getId());
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    public DocumentResponseDTO update(@PathVariable Long id,
                                      @Valid @RequestBody DocumentRequestDTO request) {
        Document updated = service.update(id, DocumentMapper.toEntity(request));
        return DocumentMapper.toDTO(updated);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/upload")
    public Document upload(@RequestParam("file") MultipartFile file,
                           @RequestParam("summary") String summary) {
        return service.handleFileUpload(file, summary);
    }
}
