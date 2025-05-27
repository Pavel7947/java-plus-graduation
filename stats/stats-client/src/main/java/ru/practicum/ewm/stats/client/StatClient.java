package ru.practicum.ewm.stats.client;

import ru.practicum.ewm.stats.protobuf.RecommendedEventProto;

import java.util.List;
import java.util.stream.Stream;

public interface StatClient {

    Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults);

    Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResult);

    Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIdList);

    void collectUserAction(long userId, long eventId, UserActionType actionType);
}
