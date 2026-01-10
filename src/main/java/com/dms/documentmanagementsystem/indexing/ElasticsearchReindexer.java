package com.dms.documentmanagementsystem.indexing;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dms.documentmanagementsystem.dto.DocumentSearchResultDTO;
import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class ElasticsearchReindexer {

    private final ElasticsearchClient elasticsearchClient;
    private final DocumentRepository documentRepository;

    public ElasticsearchReindexer(
            ElasticsearchClient elasticsearchClient,
            DocumentRepository documentRepository
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.documentRepository = documentRepository;
    }

    public void reindexAllDocuments() {
        log.info("Starting Elasticsearch reindex");

        List<Document> documents = documentRepository.findAll();
        log.info("Found {} documents to index", documents.size());

        for (Document doc : documents) {
            try {
                String contentText = doc.getContent() != null
                        ? new String(doc.getContent(), StandardCharsets.UTF_8)
                        : "";

                String reviewStatus = null;
                if (doc.getReview() != null && doc.getReview().getStatus() != null) {
                    reviewStatus = doc.getReview().getStatus().name();
                }

                DocumentSearchResultDTO esDoc =
                        new DocumentSearchResultDTO(
                                doc.getId(),
                                doc.getFileName(),
                                contentText,
                                doc.getSummary(),
                                doc.getCreatedAt(),
                                reviewStatus
                        );

                elasticsearchClient.index(i -> i
                        .index("documents")
                        .id(doc.getId().toString())
                        .document(esDoc)
                );

                log.info("Indexed document id={} fileName={}", doc.getId(), doc.getFileName());

            } catch (Exception e) {
                log.error("Failed to index document id={}", doc.getId(), e);
            }
        }

        log.info("Finished Elasticsearch reindex");
    }
}
