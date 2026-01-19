package com.dms.ocr.genai;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenAiClientTest {

    @Test
    void getSummary_skipsBlankInput() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GenAiClient client = new GenAiClient(restTemplate);
        ReflectionTestUtils.setField(client, "genAiWorkerUrl", "http://genai:8090/api/genai/summarize");

        assertNull(client.getSummary("   "));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getSummary_returnsTrimmedSummary() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GenAiClient client = new GenAiClient(restTemplate);
        ReflectionTestUtils.setField(client, "genAiWorkerUrl", "http://genai:8090/api/genai/summarize");

        when(restTemplate.postForEntity(eq("http://genai:8090/api/genai/summarize"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("  done "));

        String summary = client.getSummary("text");

        assertEquals("done", summary);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("http://genai:8090/api/genai/summarize"),
                entityCaptor.capture(), eq(String.class));

        HttpEntity entity = entityCaptor.getValue();
        assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
        assertEquals("text", ((Map<?, ?>) entity.getBody()).get("text"));
    }

    @Test
    void getSummary_returnsNullOnNon2xx() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GenAiClient client = new GenAiClient(restTemplate);
        ReflectionTestUtils.setField(client, "genAiWorkerUrl", "http://genai:8090/api/genai/summarize");

        when(restTemplate.postForEntity(eq("http://genai:8090/api/genai/summarize"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("nope"));

        assertNull(client.getSummary("text"));
    }

    @Test
    void getSummary_returnsNullOnHttp429() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GenAiClient client = new GenAiClient(restTemplate);
        ReflectionTestUtils.setField(client, "genAiWorkerUrl", "http://genai:8090/api/genai/summarize");

        when(restTemplate.postForEntity(eq("http://genai:8090/api/genai/summarize"),
                any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertNull(client.getSummary("text"));
    }

    @Test
    void getSummary_returnsNullOnRestClientFailure() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GenAiClient client = new GenAiClient(restTemplate);
        ReflectionTestUtils.setField(client, "genAiWorkerUrl", "http://genai:8090/api/genai/summarize");

        when(restTemplate.postForEntity(eq("http://genai:8090/api/genai/summarize"),
                any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("down"));

        assertNull(client.getSummary("text"));
    }
}
