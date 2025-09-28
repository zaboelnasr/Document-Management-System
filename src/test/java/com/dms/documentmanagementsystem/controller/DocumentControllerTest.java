package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.model.Document;
import com.dms.documentmanagementsystem.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DocumentService service;

    @Test
    void getAll_returnsOkPage() throws Exception {
        Document d = new Document();
        d.setId(1L);
        d.setFileName("a.pdf");

        Mockito.when(service.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(d)));

        mvc.perform(get("/api/documents").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void create_valid_returns201() throws Exception {
        Document created = new Document();
        created.setId(10L);
        created.setFileName("hello.pdf");

        Mockito.when(service.create(any(Document.class)))
                .thenReturn(created);

        String json = """
          {"fileName":"hello.pdf","summary":"first"}
        """;

        mvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/documents/10"));
    }

    @Test
    void create_invalid_missingFileName_returns400() throws Exception {
        // @Valid + @NotBlank im DTO sorgt für 400
        String json = """
          {"summary":"no filename"}
        """;

        mvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
