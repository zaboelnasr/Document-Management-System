package com.dms.documentmanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReviewStatusRequest {
    @NotBlank
    private String status;
}