package com.dummby.interviewscheduler.service.whatsapp;

import com.dummby.interviewscheduler.config.WhatsAppProperties;
import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class LocalWhatsAppProvider implements WhatsAppProvider {

    private final WhatsAppProperties props;
    private final WebClient client;

    public LocalWhatsAppProvider(WhatsAppProperties props,
                                 @Qualifier("waBridgeClient") WebClient client) {
        this.props = props;
        this.client = client;
    }

    @Override
    public WhatsAppSendResult send(String toPhone, String message) {
        try {
            Map<String, Object> body = Map.of("to", toPhone, "message", message);

            // exchangeToMono lets us read the JSON error body even on 4xx/5xx
            // statuses (e.g. 404 = "number not registered", 503 = "not connected"),
            // so we surface the bridge's REAL reason instead of a generic
            // "404 Not Found" that looks like the bridge is down.
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/send")
                    .bodyValue(body)
                    .exchangeToMono(resp -> resp.bodyToMono(Map.class)
                            .defaultIfEmpty(Map.of())
                            .map(b -> {
                                ((Map<String, Object>) b).put("_httpStatus", resp.statusCode().value());
                                return (Map<String, Object>) b;
                            }))
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (response == null) {
                return WhatsAppSendResult.fail("Empty response from wa-bridge");
            }
            if (Boolean.TRUE.equals(response.get("success"))) {
                Object id = response.get("id");
                return WhatsAppSendResult.ok(id == null ? null : String.valueOf(id));
            }

            // Build a clear, human-friendly error from the bridge's JSON body.
            String bridgeError = response.get("error") == null ? null : String.valueOf(response.get("error"));
            Object statusObj = response.get("_httpStatus");
            int httpStatus = statusObj instanceof Integer ? (Integer) statusObj : 0;

            String friendly = friendlyError(httpStatus, bridgeError, toPhone);
            log.warn("wa-bridge send failed for {} (HTTP {}): {}", toPhone, httpStatus, bridgeError);
            return WhatsAppSendResult.fail(friendly);

        } catch (WebClientRequestException connEx) {
            // True connection failure — bridge process is actually down.
            log.error("wa-bridge connection failed for {}: {}", toPhone, connEx.getMessage());
            return WhatsAppSendResult.fail(
                    "WhatsApp bridge is not running. Start it (cd wa-bridge && npm start) and link a device.");
        } catch (Exception e) {
            log.error("Local WhatsApp send failed for {}: {}", toPhone, e.getMessage());
            return WhatsAppSendResult.fail("Send error: " + e.getMessage());
        }
    }

    /** Translate the bridge HTTP status + body into a clear message for HR. */
    private String friendlyError(int httpStatus, String bridgeError, String phone) {
        if (bridgeError != null && !bridgeError.isBlank()) {
            // The bridge already gives a precise reason — prefer it.
            if (bridgeError.toLowerCase().contains("not registered")) {
                return "This number is not on WhatsApp: " + phone;
            }
            if (bridgeError.toLowerCase().contains("not connected")) {
                return "WhatsApp not linked. Scan the QR on the WhatsApp page first.";
            }
            return bridgeError;
        }
        return switch (httpStatus) {
            case 404 -> "This number is not on WhatsApp: " + phone;
            case 503 -> "WhatsApp not linked. Scan the QR on the WhatsApp page first.";
            case 400 -> "Invalid phone number: " + phone;
            default  -> "Send failed (HTTP " + httpStatus + ")";
        };
    }

    @Override public String name() { return "local"; }
}

