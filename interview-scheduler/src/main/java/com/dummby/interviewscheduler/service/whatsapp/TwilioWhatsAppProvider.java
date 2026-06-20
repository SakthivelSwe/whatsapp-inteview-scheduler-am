package com.dummby.interviewscheduler.service.whatsapp;

import com.dummby.interviewscheduler.config.WhatsAppProperties;
import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Twilio WhatsApp provider.
 * Docs: https://www.twilio.com/docs/whatsapp/api
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioWhatsAppProvider implements WhatsAppProvider {

    private static final String BASE = "https://api.twilio.com/2010-04-01";
    private final WhatsAppProperties props;
    private WebClient client;

    private WebClient client() {
        if (client == null) {
            WhatsAppProperties.Twilio t = props.getTwilio();
            String auth = Base64.getEncoder().encodeToString(
                    (t.getAccountSid() + ":" + t.getAuthToken()).getBytes(StandardCharsets.UTF_8));
            client = WebClient.builder()
                    .baseUrl(BASE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .build();
        }
        return client;
    }

    @Override
    public WhatsAppSendResult send(String toPhone, String message) {
        try {
            WhatsAppProperties.Twilio t = props.getTwilio();
            String to = toPhone.startsWith("+") ? toPhone : "+" + toPhone;

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", t.getFromNumber());
            form.add("To", "whatsapp:" + to);
            form.add("Body", message);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri("/Accounts/{sid}/Messages.json", t.getAccountSid())
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String sid = response == null ? null : String.valueOf(response.get("sid"));
            return WhatsAppSendResult.ok(sid);
        } catch (Exception e) {
            log.error("Twilio WhatsApp send failed for {}: {}", toPhone, e.getMessage());
            return WhatsAppSendResult.fail(e.getMessage());
        }
    }

    @Override public String name() { return "twilio"; }
}

