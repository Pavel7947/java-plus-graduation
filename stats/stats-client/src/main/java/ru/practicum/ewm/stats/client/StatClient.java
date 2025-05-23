package ru.practicum.ewm.stats.client;

import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.StatsDto;
import ru.practicum.ewm.stats.protobuf.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface StatClient {

    Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults);

    Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResult);

    Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIdList);

    String saveHit(EndpointHitDto requestBody);

    List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, String uris, boolean unique);
}
