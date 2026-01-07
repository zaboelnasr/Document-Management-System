package com.dms.dto;

public record OcrResultEvent(
        Long documentId,
        String fileName,
        String text,
        String summary
) {}
