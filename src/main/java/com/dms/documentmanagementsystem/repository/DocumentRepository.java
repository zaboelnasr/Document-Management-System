package com.dms.documentmanagementsystem.repository;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("select d from Document d join d.review r where r.status = :status")
    Page<Document> findByReviewStatus(@Param("status") ReviewStatus status, Pageable pageable);
}