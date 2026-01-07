package com.dms.dto;

public record DocumentIndexDTO(
        Long id,
        String fileName,
        String content,
        String summary
) {}
