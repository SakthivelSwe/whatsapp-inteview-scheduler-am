package com.dummby.interviewscheduler.model.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppSendResult {
    private boolean success;
    private String providerMessageId;
    private String errorMessage;

    public static WhatsAppSendResult ok(String id) {
        return WhatsAppSendResult.builder().success(true).providerMessageId(id).build();
    }
    public static WhatsAppSendResult fail(String err) {
        return WhatsAppSendResult.builder().success(false).errorMessage(err).build();
    }
}

