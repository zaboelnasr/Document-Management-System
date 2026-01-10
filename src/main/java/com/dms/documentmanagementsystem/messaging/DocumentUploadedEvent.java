package com.dms.documentmanagementsystem.messaging;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DocumentUploadedEvent implements Serializable {

    @Setter @Getter private Long id;
    @Setter @Getter private String fileName;
    @Setter @Getter private String summary;
    @Setter @Getter private LocalDateTime createdAt;
    @Setter @Getter private String bucket;
    @Setter @Getter private String objectKey;
    @Setter @Getter private String reviewStatus;

    public DocumentUploadedEvent() {}

    public DocumentUploadedEvent(
            Long id,
            String fileName,
            String summary,
            LocalDateTime createdAt,
            String bucket,
            String objectKey,
            String reviewStatus
    ) {
        this.id = id;
        this.fileName = fileName;
        this.summary = summary;
        this.createdAt = createdAt;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.reviewStatus = reviewStatus;
    }

    @Override
    public String toString() {
        return "DocumentUploadedEvent{" +
                "id=" + id +
                ", filename=" + fileName +
                ", summary=" + summary +
                ", createdAt=" + createdAt +
                ", bucket='" + bucket + '\'' +
                ", objectKey='" + objectKey + '\'' +
                ", reviewStatus='" + reviewStatus + '\'' +
                '}';
    }
}
