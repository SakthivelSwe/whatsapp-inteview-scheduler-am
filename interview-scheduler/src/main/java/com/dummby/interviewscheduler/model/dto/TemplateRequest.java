package com.dummby.interviewscheduler.model.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateRequest {
    private String name;
    private String body;
    private boolean active;
}

