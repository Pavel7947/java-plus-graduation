package ru.practicum.ewm.stats.server.service;

import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);

    void saveHit(EndpointHitDto endpointHitDto);
}
