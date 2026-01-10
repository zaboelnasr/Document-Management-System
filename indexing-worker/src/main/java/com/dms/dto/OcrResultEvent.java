package com.dms.dto;

import java.time.LocalDateTime;

public record OcrResultEvent(
        Long documentId,
        String fileName,
        String text,
        String summary,
        LocalDateTime createdAt,
        String reviewStatus
) {}
