package com.dummby.interviewscheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class KeepAliveService {

    // Render automatically injects RENDER_EXTERNAL_URL containing the public URL of the web service
    @Value("${RENDER_EXTERNAL_URL:http://localhost:8080}")
    private String selfUrl;

    @Value("${whatsapp.local.base-url:http://localhost:3000}")
    private String waBridgeUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Runs every 14 minutes (14 * 60 * 1000 = 840000 ms)
     * This hits the public URLs of both the Spring Boot app and WA Bridge
     * to prevent Render's free tier from sleeping (which happens after 15 mins of inactivity).
     */
    @Scheduled(fixedRate = 840000)
    public void pingToKeepAlive() {
        log.info("[KeepAlive] Executing scheduled ping to prevent Render instances from sleeping...");

        // 1. Ping self (Spring Boot Backend)
        try {
            String selfHealthUrl = selfUrl + "/actuator/health";
            restTemplate.getForObject(selfHealthUrl, String.class);
            log.info("[KeepAlive] Successfully pinged self: {}", selfHealthUrl);
        } catch (Exception e) {
            log.warn("[KeepAlive] Failed to ping self: {}", e.getMessage());
        }

        // 2. Ping WA-Bridge
        try {
            if (waBridgeUrl != null && !waBridgeUrl.isEmpty() && waBridgeUrl.startsWith("http")) {
                String bridgeHealthUrl = waBridgeUrl + "/status";
                restTemplate.getForObject(bridgeHealthUrl, String.class);
                log.info("[KeepAlive] Successfully pinged wa-bridge: {}", bridgeHealthUrl);
            }
        } catch (Exception e) {
            log.warn("[KeepAlive] Failed to ping wa-bridge: {}", e.getMessage());
        }
    }
}
