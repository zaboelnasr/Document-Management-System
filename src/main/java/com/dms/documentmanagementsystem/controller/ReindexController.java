package com.dms.documentmanagementsystem.controller;

import com.dms.documentmanagementsystem.indexing.ElasticsearchReindexer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ReindexController {

    private final ElasticsearchReindexer reindexer;

    /**
     * Manually reindex all documents from DB into Elasticsearch
     */
    @PostMapping("/reindex")
    public ResponseEntity<Void> reindex() {
        reindexer.reindexAllDocuments();
        return ResponseEntity.ok().build();
    }
}
