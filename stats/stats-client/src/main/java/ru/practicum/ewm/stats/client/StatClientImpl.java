package ru.practicum.ewm.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.StatsDto;
import ru.practicum.ewm.stats.exceptions.RestClientRuntimeException;
import ru.practicum.ewm.stats.exceptions.StatsServerUnavailable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StatClientImpl implements StatClient {

    private RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final String statsServerName;

    public StatClientImpl(DiscoveryClient discoveryClient, @Value("${stats.server.name}") String statsServerName) {
        this.discoveryClient = discoveryClient;
        this.statsServerName = statsServerName;
    }

    public String saveHit(EndpointHitDto requestBody) {
        if (restClient == null) {
            restClient = getRestClient();
        }
        try {
            return restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new RestClientRuntimeException(response.getStatusCode(), response.getBody().toString());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new RestClientRuntimeException(response.getStatusCode(), response.getBody().toString());
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            log.debug("При сохранении статистики сервис оказался недоступным. Делаем запрос на обнаружение...");
            restClient = getRestClient();
            return saveHit(requestBody);
        }
    }

    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, String uris, boolean unique) {
        if (restClient == null) {
            restClient = getRestClient();
        }
        Map<String, Object> requestParams = Map.of("start", start, "end", end, "uris", uris,
                "unique", unique);

        UriComponents uriComponents = UriComponentsBuilder
                .fromUriString("/stats?start={start}&end={end}&uris={uris}&unique={unique}")
                .build().expand(requestParams);
        try {
            return restClient.get()
                    .uri(uriComponents.toUriString())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new RestClientRuntimeException(response.getStatusCode(), response.getBody().toString());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new RestClientRuntimeException(response.getStatusCode(), response.getBody().toString());
                    })
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (ResourceAccessException e) {
            log.debug("При получении статистики сервис оказался недоступным. Делаем запрос на обнаружение...");
            restClient = getRestClient();
            return getStats(start, end, uris, unique);
        }
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient
                .getInstances(statsServerName);
        if (instances.isEmpty()) {
            throw new StatsServerUnavailable("Ошибка обнаружения сервиса статистики");
        }
        return instances.getFirst();
    }

    private RestClient getRestClient() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .fixedBackoff(3000L)
                .maxAttempts(3)
                .retryOn(StatsServerUnavailable.class)
                .build();
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return RestClient.builder().baseUrl("http://" + instance.getHost() + ":" + instance.getPort()).build();
    }
}
