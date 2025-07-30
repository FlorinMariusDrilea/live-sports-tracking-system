package com.sports.livesportstrackingsystem.external;

import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class ExternalApiService {
    private final WebClient webClient;
    @Value("${external.api.url}")
    private String externalApiUrl;

    public ExternalApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("").build();
    }

    public Mono<EventExternalApiResponse> getEventScore(String eventId) {
        String url = externalApiUrl.replace("{eventId}", eventId);
        log.debug("Calling external API: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(EventExternalApiResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(3))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying external API call for eventId {}. Attempt -{}", eventId, retrySignal.totalRetriesInARow())))
                .onErrorResume(e -> {
                    log.error("Failed to retrieve event score for eventId {} from external API: {}", eventId, e.getMessage(), e);
                    return Mono.error(new RuntimeException("External API call failed for eventId: " + eventId, e));
                });
    }
}
