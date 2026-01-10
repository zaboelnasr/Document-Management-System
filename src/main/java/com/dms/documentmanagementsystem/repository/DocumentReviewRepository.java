package com.dms.documentmanagementsystem.repository;

import com.dms.documentmanagementsystem.model.DocumentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentReviewRepository extends JpaRepository<DocumentReview, Long> {
    Optional<DocumentReview> findByDocumentId(Long documentId);
}