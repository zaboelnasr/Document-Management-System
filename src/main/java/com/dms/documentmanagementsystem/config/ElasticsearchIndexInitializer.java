package com.dms.documentmanagementsystem.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient client;

    public ElasticsearchIndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            boolean exists = client.indices().exists(e -> e.index("documents")).value();

            if (!exists) {
                client.indices().create(c -> c
                        .index("documents")
                        .mappings(m -> m
                                .properties("fileName", p -> p.text(t -> t))
                                .properties("content", p -> p.text(t -> t))
                                .properties("summary", p -> p.text(t -> t))
                        )
                );
                log.info("Created Elasticsearch index: documents");
            } else {
                log.info("Elasticsearch index already exists: documents");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Elasticsearch index", e);
        }
    }
}
