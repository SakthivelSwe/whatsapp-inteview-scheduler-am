package com.dummby.interviewscheduler.service.whatsapp;

import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;

public interface WhatsAppProvider {
    /**
     * Send a WhatsApp text message to the given international-format phone number.
     * @param toPhone phone number (digits, optionally with +)
     * @param message resolved message body
     */
    WhatsAppSendResult send(String toPhone, String message);

    String name();
}

