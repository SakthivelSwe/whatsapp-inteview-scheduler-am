package com.dummby.interviewscheduler.service.whatsapp;

import com.dummby.interviewscheduler.config.WhatsAppProperties;
import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Routes WhatsApp send calls to the configured provider. */
@Slf4j
@Service
public class WhatsAppService {

    private final WhatsAppProperties props;
    private final Map<String, WhatsAppProvider> providers;

    public WhatsAppService(WhatsAppProperties props, List<WhatsAppProvider> providerList) {
        this.props = props;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(WhatsAppProvider::name, Function.identity()));
        log.info("Available WhatsApp providers: {} | active='{}'", providers.keySet(), props.getProvider());
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public WhatsAppSendResult send(String phone, String message) {
        WhatsAppProvider provider = providers.get(props.getProvider());
        if (provider == null) {
            throw new IllegalStateException("No WhatsApp provider configured for: " + props.getProvider());
        }
        WhatsAppSendResult result = provider.send(phone, message);
        if (!result.isSuccess()) {
            // Throwing triggers @Retryable to retry
            throw new RuntimeException("Send failed: " + result.getErrorMessage());
        }
        return result;
    }
}

