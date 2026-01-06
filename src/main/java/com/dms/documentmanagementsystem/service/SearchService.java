package com.dms.documentmanagementsystem.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.dms.documentmanagementsystem.dto.DocumentSearchResultDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final ElasticsearchClient client;

    public SearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public List<DocumentSearchResultDTO> search(String term) {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index("documents")
                    .query(q -> q.multiMatch(m -> m
                            .fields(
                                    "fileName",
                                    "fileName.keyword",
                                    "summary",
                                    "content"
                            )
                            .query(term)
                    ))

            );

            SearchResponse<DocumentSearchResultDTO> response =
                    client.search(request, DocumentSearchResultDTO.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (ElasticsearchException e) {
            if ("index_not_found_exception".equals(e.error().type())) {
                return Collections.emptyList();
            }
            throw new RuntimeException("Elasticsearch search failed", e);

        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch search failed", e);
        }
    }
}
