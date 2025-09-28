package com.dms.documentmanagementsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@ToString(exclude = "content")
@Data @NoArgsConstructor @AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String summary;

    @Lob
    private byte[] content;  // store file bytes for now

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Setting create Time of Documents before created
     */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Setting update Time of Documents before updated
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
