package com.dms.ocr.event;

public record OcrResultEvent(
        Long documentId,
        String fileName,
        String text,
        String summary
) {}
