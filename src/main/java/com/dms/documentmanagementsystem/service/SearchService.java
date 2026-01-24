package com.dms.documentmanagementsystem.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
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
            String query = buildWildcardQuery(term);
            SearchRequest request = SearchRequest.of(s -> s
                    .index("documents")
                    .query(q -> q.queryString(qs -> qs
                            .fields(
                                    "fileName",
                                    "summary",
                                    "content"
                            )
                            .query(query)
                            .defaultOperator(Operator.And)
                            .analyzeWildcard(true)
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

    private String buildWildcardQuery(String term) {
        if (term == null) {
            return "*";
        }
        String trimmed = term.trim();
        if (trimmed.isEmpty()) {
            return "*";
        }
        String[] parts = trimmed.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (builder.length() > 0) {
                builder.append(" AND ");
            }
            builder.append('*').append(part).append('*');
        }
        return builder.toString();
    }
}
