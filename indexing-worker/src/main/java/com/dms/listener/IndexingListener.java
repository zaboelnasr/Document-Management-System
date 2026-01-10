package com.dms.listener;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.dms.ElasticsearchConstants;

import java.io.IOException;
import java.util.Map;

@Component
public class IndexingListener {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public IndexingListener(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @RabbitListener(queues = "${DMS_RMQ_QUEUE_INDEX}")
    public void handleIndexMessage(String messageJson) throws IOException {
        Map<String, Object> msg = mapper.readValue(messageJson, Map.class);

        Map<String, Object> doc = Map.of(
                "id", msg.get("documentId"),
                "fileName", msg.get("fileName"),
                "content", msg.get("content"),
                "summary", msg.get("summary"),
                "createdAt", msg.get("createdAt"),
                "reviewStatus", msg.get("reviewStatus")
        );


        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(ElasticsearchConstants.DOCUMENT_INDEX)
                .id(msg.get("documentId").toString())
                .document(doc)
        );

        elasticsearchClient.index(request);
    }
}
