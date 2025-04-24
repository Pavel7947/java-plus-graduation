package ru.practicum.ewm.stats.client;

import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatClient {

    String saveHit(EndpointHitDto requestBody);

    List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, String uris, boolean unique);
}
