package com.dms.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResult {
    private Long id;
    private String fileName;
    private String content;
    private String summary;
}