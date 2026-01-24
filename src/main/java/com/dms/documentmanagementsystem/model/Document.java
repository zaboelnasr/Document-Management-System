package com.dms.documentmanagementsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@ToString(exclude = {"content", "review"})
@EqualsAndHashCode(exclude = {"content", "review"})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String bucket;
    private String objectKey;
    private String contentType;
    private Long size;

    @Lob
    private byte[] content;  // store file bytes for now

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private OcrStatus ocrStatus;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DocumentReview review;

    /**
     * Setting create Time of Documents before created
     */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (ocrStatus == null) {
            ocrStatus = OcrStatus.PENDING;
        }
    }

    /**
     * Setting update Time of Documents before updated
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
