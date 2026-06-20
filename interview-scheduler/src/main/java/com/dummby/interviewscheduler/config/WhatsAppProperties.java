package com.dummby.interviewscheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "whatsapp")
@Getter @Setter
public class WhatsAppProperties {
    private String provider = "mock";
    private Meta meta = new Meta();
    private Twilio twilio = new Twilio();
    private Local local = new Local();
    private RateLimit rateLimit = new RateLimit();

    @Getter @Setter
    public static class Local {
        /** Base URL of the wa-bridge Node service (whatsapp-web.js). */
        private String baseUrl = "http://localhost:3000";
    }

    @Getter @Setter
    public static class Meta {
        private String apiUrl;
        private String phoneNumberId;
        private String accessToken;
        private String templateName;
        private String templateLanguage = "en_US";
    }

    @Getter @Setter
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    }

    @Getter @Setter
    public static class RateLimit {
        /** Per RD §2.B: configurable per-message delay (1-2s recommended). Takes precedence if > 0. */
        private double delaySeconds = 1.5;
        private int messagesPerSecond = 0; // fallback if delaySeconds <= 0
        private int maxConcurrent = 20;
        /**
         * Random jitter added to each delay to make traffic look more human-like
         * (anti-ban). Value is a fraction 0.0–1.0 of delaySeconds; e.g. 0.2 means
         * actual gap is randomly between 80%–120% of delaySeconds.
         */
        private double jitter = 0.2;
        /**
         * Safety cap: do not send more than this many messages in any rolling
         * 24-hour window from the same provider. 0 = no cap. Recommended 150
         * for fresh linked numbers, 500 for warmed-up business numbers.
         */
        private int dailyLimit = 0;
    }
}

