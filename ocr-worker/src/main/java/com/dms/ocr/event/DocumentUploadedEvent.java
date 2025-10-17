package com.dms.ocr.event;

import java.time.LocalDateTime;

public class DocumentUploadedEvent {

    private Long id;
    private String fileName;
    private String summary;
    private LocalDateTime createdAt;

    // Default constructor (needed for Jackson deserialization)
    public DocumentUploadedEvent() {
    }

    public DocumentUploadedEvent(Long id, String fileName, String summary, LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "DocumentUploadedEvent{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", summary='" + summary + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
