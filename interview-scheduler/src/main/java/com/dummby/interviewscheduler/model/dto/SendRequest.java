package com.dummby.interviewscheduler.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SendRequest {
    @NotNull
    private UUID batchId;

    /** Template name to use. If null, uses the default 'active' template. */
    private String templateName;
}

