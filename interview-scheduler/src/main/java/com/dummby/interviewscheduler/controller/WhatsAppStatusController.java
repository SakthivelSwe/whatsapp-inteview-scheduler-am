package com.dummby.interviewscheduler.controller;

import com.dummby.interviewscheduler.config.WhatsAppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppStatusController {

    private final WhatsAppProperties props;
    private final WebClient bridgeClient;

    public WhatsAppStatusController(WhatsAppProperties props,
                                    @Qualifier("waBridgeClient") WebClient bridgeClient) {
        this.props = props;
        this.bridgeClient = bridgeClient;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        String provider = props.getProvider();
        String baseUrl = props.getLocal().getBaseUrl();

        if (!"local".equalsIgnoreCase(provider)) {
            return ResponseEntity.ok(Map.of(
                    "provider", provider,
                    "ready", true,
                    "hasQr", false,
                    "bridgeUrl", baseUrl,
                    "qrUrl", baseUrl + "/qr",
                    "message", "Active provider '" + provider + "' does not require QR linking."
            ));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> bridge = bridgeClient
                    .get().uri("/status")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("provider", "local");
            out.put("ready", bridge != null && Boolean.TRUE.equals(bridge.get("ready")));
            out.put("hasQr", bridge != null && Boolean.TRUE.equals(bridge.get("hasQr")));
            out.put("state", bridge == null ? "UNKNOWN" : bridge.getOrDefault("state", "UNKNOWN"));
            Object info = bridge == null ? null : bridge.get("info");
            out.put("info", info == null ? java.util.Map.of() : info);
            out.put("bridgeUrl", baseUrl);
            out.put("qrUrl", baseUrl + "/qr");
            out.put("qrImageUrl", baseUrl + "/qr.png");
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            log.warn("wa-bridge unreachable at {}: {}", baseUrl, e.getMessage());
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("provider", "local");
            out.put("ready", false);
            out.put("hasQr", false);
            out.put("bridgeUrl", baseUrl);
            out.put("qrUrl", baseUrl + "/qr");
            out.put("error", "wa-bridge not reachable: " + e.getMessage());
            return ResponseEntity.ok(out);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        String baseUrl = props.getLocal().getBaseUrl();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = bridgeClient
                    .post().uri("/logout")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("success", resp != null && Boolean.TRUE.equals(resp.get("success")));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("success", false);
            out.put("error", e.getMessage());
            return ResponseEntity.ok(out);
        }
    }

    /**
     * Hard reset the bridge: wipes the saved session and forces a brand-new QR.
     * Use this when the QR is stuck / not appearing.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = bridgeClient
                    .post().uri("/reset")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("success", resp != null && Boolean.TRUE.equals(resp.get("success")));
            out.put("message", resp == null ? null : resp.get("message"));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            log.warn("Reset failed: {}", e.getMessage());
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("success", false);
            out.put("error", e.getMessage());
            return ResponseEntity.ok(out);
        }
    }
}

