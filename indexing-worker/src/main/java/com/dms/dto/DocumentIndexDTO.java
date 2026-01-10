package com.dms.dto;

import java.time.LocalDateTime;

public record DocumentIndexDTO(
        Long id,
        String fileName,
        String content,
        String summary,
        LocalDateTime createdAt,
        String reviewStatus
) {}