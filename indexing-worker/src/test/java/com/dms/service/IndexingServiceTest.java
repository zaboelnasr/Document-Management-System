package com.dms.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dms.dto.DocumentIndexDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class IndexingServiceTest {

    @Test
    void index_callsElasticsearchClient() throws Exception {
        // Arrange
        ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class);
        IndexingService service = new IndexingService(client);

        DocumentIndexDTO dto = new DocumentIndexDTO(
                1L,
                "test.pdf",
                "OCR TEXT CONTENT",
                "summary",
                java.time.LocalDateTime.now(),
                "OPEN"
        );

        // Act
        service.index(dto);

        // Assert
        verify(client).index(any(Function.class));
    }
}
