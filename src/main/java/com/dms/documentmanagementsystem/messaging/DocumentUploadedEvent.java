package com.dms.documentmanagementsystem.messaging;

import java.time.LocalDateTime;

public record DocumentUploadedEvent(
        Long id,
        String fileName,
        String summary,
        LocalDateTime createdAt
) {}
