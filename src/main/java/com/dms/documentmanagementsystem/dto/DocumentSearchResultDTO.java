package com.dms.documentmanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResultDTO {
    private Long id;
    private String fileName;
    private String content;
    private String summary;
    private LocalDateTime createdAt;
    private String reviewStatus;
}
