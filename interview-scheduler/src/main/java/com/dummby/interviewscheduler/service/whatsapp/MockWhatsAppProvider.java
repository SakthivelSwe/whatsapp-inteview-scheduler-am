package com.dummby.interviewscheduler.service.whatsapp;

import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Mock provider: logs the message instead of calling a real API. Useful for local dev. */
@Slf4j
@Component
public class MockWhatsAppProvider implements WhatsAppProvider {
    @Override
    public WhatsAppSendResult send(String toPhone, String message) {
        log.info("[MOCK-WHATSAPP] to={} | message=\n{}", toPhone, message);
        return WhatsAppSendResult.ok("mock-" + UUID.randomUUID());
    }

    @Override public String name() { return "mock"; }
}

