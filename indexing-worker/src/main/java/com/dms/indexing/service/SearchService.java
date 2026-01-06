package com.dms.indexing.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.dms.indexing.DocumentSearchResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service

public class SearchService {

    private final ElasticsearchClient elasticsearchClient;

    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<DocumentSearchResult> search(String term) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("documents")
                    .query(q -> q.multiMatch(m -> m
                            .fields("fileName", "content", "summary")
                            .query(term)
                    ))
            );

            SearchResponse<DocumentSearchResult> response =
                    elasticsearchClient.search(searchRequest, DocumentSearchResult.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Failed to search in Elasticsearch", e);
        }
    }
}
