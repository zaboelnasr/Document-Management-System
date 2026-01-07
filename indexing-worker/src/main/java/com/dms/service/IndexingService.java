package com.dms.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dms.dto.DocumentIndexDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final ElasticsearchClient elasticsearchClient;

    public void index(DocumentIndexDTO doc) {
        try {
            elasticsearchClient.index(i -> i
                    .index("documents")
                    .id(doc.id().toString())
                    .document(doc)
            );
            log.info("Indexed OCR document id={} into Elasticsearch", doc.id());
        } catch (Exception e) {
            log.error("Failed to index document id={}", doc.id(), e);
        }
    }
}
