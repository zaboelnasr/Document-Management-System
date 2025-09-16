package com.dms.documentmanagementsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
