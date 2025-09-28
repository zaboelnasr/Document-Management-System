package com.dms.documentmanagementsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class DocumentResponseDTO {
    // getters/setters
    private Long id;
    private String fileName;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
