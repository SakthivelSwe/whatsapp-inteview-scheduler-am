package com.dummby.interviewscheduler.service.whatsapp;

import com.dummby.interviewscheduler.config.WhatsAppProperties;
import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Meta WhatsApp Cloud API provider.
 *
 * NOTE: For business-initiated outbound messages, Meta requires a pre-approved
 * template. Here we send a 'text' message which only works inside a 24-hour
 * customer service window. For production interview invites, switch to
 * the 'template' payload format and reference your approved template name.
 *
 * Docs: https://developers.facebook.com/docs/whatsapp/cloud-api/reference/messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetaWhatsAppProvider implements WhatsAppProvider {

    private final WhatsAppProperties props;
    private WebClient client;

    private WebClient client() {
        if (client == null) {
            WhatsAppProperties.Meta meta = props.getMeta();
            client = WebClient.builder()
                    .baseUrl(meta.getApiUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + meta.getAccessToken())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
        return client;
    }

    @Override
    public WhatsAppSendResult send(String toPhone, String message) {
        try {
            String phoneId = props.getMeta().getPhoneNumberId();
            String to = toPhone.startsWith("+") ? toPhone.substring(1) : toPhone;

            Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "to", to,
                    "type", "text",
                    "text", Map.of("body", message)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri("/{phoneId}/messages", phoneId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String msgId = extractMessageId(response);
            return WhatsAppSendResult.ok(msgId);
        } catch (Exception e) {
            log.error("Meta WhatsApp send failed for {}: {}", toPhone, e.getMessage());
            return WhatsAppSendResult.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) return null;
        Object messages = response.get("messages");
        if (messages instanceof java.util.List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) return String.valueOf(m.get("id"));
        }
        return null;
    }

    @Override public String name() { return "meta"; }
}

