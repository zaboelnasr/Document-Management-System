package com.dms.ocr.event;

import java.time.LocalDateTime;

public record OcrResultEvent(
        Long documentId,
        String fileName,
        String text,
        String summary,
        LocalDateTime createdAt,
        String reviewStatus
) {}
