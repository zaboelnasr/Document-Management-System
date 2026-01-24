package com.dms.ocr.listener;

import com.dms.ocr.backend.BackendClient;
import com.dms.ocr.event.DocumentUploadedEvent;
import com.dms.ocr.event.OcrResultEvent;
import com.dms.ocr.genai.GenAiClient;
import com.dms.ocr.service.OcrEngine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OcrListenerTest {

    @Test
    void onMessage_runsOcrAndPublishesResult() throws Exception {
        S3Client s3 = Mockito.mock(S3Client.class);
        GenAiClient genAiClient = Mockito.mock(GenAiClient.class);
        BackendClient backendClient = Mockito.mock(BackendClient.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        OcrEngine ocrEngine = Mockito.mock(OcrEngine.class);

        ResponseInputStream<GetObjectResponse> stream = Mockito.mock(ResponseInputStream.class);
        when(stream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        when(stream.read()).thenReturn(-1);
        when(s3.getObject(Mockito.<Consumer<GetObjectRequest.Builder>>any())).thenReturn(stream);

        when(ocrEngine.extractText(any(), any())).thenReturn("some text");
        when(genAiClient.getSummary("some text")).thenReturn("summary");

        OcrListener listener = new OcrListener(s3, genAiClient, backendClient, rabbitTemplate, ocrEngine);
        ReflectionTestUtils.setField(listener, "exchange", "dms.exchange");
        ReflectionTestUtils.setField(listener, "ocrResultRoutingKey", "ocr.result");

        DocumentUploadedEvent event = new DocumentUploadedEvent(
                1L,
                "doc.pdf",
                "summary",
                LocalDateTime.now(),
                "documents",
                "key",
                "OPEN"
        );

        listener.onMessage(event);

        verify(backendClient).updateSummary(eq(1L), eq("summary"));
        ArgumentCaptor<OcrResultEvent> captor = ArgumentCaptor.forClass(OcrResultEvent.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());
        assertEquals(1L, captor.getValue().documentId());
        assertEquals("doc.pdf", captor.getValue().fileName());
    }

    @Test
    void onMessage_skipsPublishWhenNoText() throws Exception {
        S3Client s3 = Mockito.mock(S3Client.class);
        GenAiClient genAiClient = Mockito.mock(GenAiClient.class);
        BackendClient backendClient = Mockito.mock(BackendClient.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        OcrEngine ocrEngine = Mockito.mock(OcrEngine.class);

        ResponseInputStream<GetObjectResponse> stream = Mockito.mock(ResponseInputStream.class);
        when(stream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        when(stream.read()).thenReturn(-1);
        when(s3.getObject(Mockito.<Consumer<GetObjectRequest.Builder>>any())).thenReturn(stream);
        when(ocrEngine.extractText(any(), any())).thenReturn("");

        OcrListener listener = new OcrListener(s3, genAiClient, backendClient, rabbitTemplate, ocrEngine);
        ReflectionTestUtils.setField(listener, "exchange", "dms.exchange");
        ReflectionTestUtils.setField(listener, "ocrResultRoutingKey", "ocr.result");

        DocumentUploadedEvent event = new DocumentUploadedEvent(
                2L,
                "doc.pdf",
                "summary",
                LocalDateTime.now(),
                "documents",
                "key",
                "OPEN"
        );

        listener.onMessage(event);

        verify(rabbitTemplate, never())
                .convertAndSend(anyString(), anyString(), Mockito.any(Object.class));
        verify(backendClient, never()).updateSummary(anyLong(), anyString());
    }
}
