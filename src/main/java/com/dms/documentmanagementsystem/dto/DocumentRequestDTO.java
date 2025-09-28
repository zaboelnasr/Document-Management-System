package com.dms.documentmanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;

public class DocumentRequestDTO {
    @NotBlank
    private String fileName;

    private String summary; // optional

    // getters/setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
