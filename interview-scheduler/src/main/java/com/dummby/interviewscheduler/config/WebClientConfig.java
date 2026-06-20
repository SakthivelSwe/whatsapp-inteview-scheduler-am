package com.dummby.interviewscheduler.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Singleton WebClient beans. Creating a new WebClient per request (as the
 * earlier code did) creates a fresh Netty connection pool each time which
 * is wasteful and adds 50-200ms of latency. These shared instances reuse
 * connections and are configured with sane timeouts.
 */
@Configuration
public class WebClientConfig {

    /**
     * General-purpose client used by status checks. Short timeouts so a
     * non-responsive bridge doesn't block the UI.
     */
    @Bean
    @Qualifier("waBridgeClient")
    public WebClient waBridgeClient(WhatsAppProperties props) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(15))
                .compress(true);
        return WebClient.builder()
                .baseUrl(props.getLocal().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Shared client for outbound providers (Meta etc.) — kept generic. */
    @Bean
    @Qualifier("genericHttpClient")
    public WebClient genericHttpClient() {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(30))
                .compress(true);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

