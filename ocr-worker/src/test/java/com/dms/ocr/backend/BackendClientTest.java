package com.dms.ocr.backend;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendClientTest {

    @Test
    void updateSummary_sendsJsonRequest() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BackendClient client = new BackendClient(restTemplate);
        ReflectionTestUtils.setField(client, "backendBaseUrl", "http://backend:8080");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        client.updateSummary(5L, "hello");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(Void.class));

        assertEquals("http://backend:8080/api/documents/5/summary", urlCaptor.getValue());
        HttpEntity entity = entityCaptor.getValue();
        assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
        assertEquals("hello", ((Map<?, ?>) entity.getBody()).get("summary"));
    }

    @Test
    void updateSummary_throwsOnClientError() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BackendClient client = new BackendClient(restTemplate);
        ReflectionTestUtils.setField(client, "backendBaseUrl", "http://backend:8080");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(IllegalStateException.class, () -> client.updateSummary(1L, "x"));
    }
}
