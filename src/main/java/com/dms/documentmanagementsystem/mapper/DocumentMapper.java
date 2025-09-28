package com.dms.documentmanagementsystem.mapper;

import com.dms.documentmanagementsystem.dto.DocumentRequestDTO;
import com.dms.documentmanagementsystem.dto.DocumentResponseDTO;
import com.dms.documentmanagementsystem.model.Document;

public class DocumentMapper {
    private DocumentMapper() {}

    public static Document toEntity(DocumentRequestDTO dto) {
        Document d = new Document();
        d.setFileName(dto.getFileName());
        d.setSummary(dto.getSummary());
        return d;
    }

    public static DocumentResponseDTO toDTO(Document entity) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setSummary(entity.getSummary());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
