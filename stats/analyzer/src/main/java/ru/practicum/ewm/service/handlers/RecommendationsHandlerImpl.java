package ru.practicum.ewm.service.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.WeightSum;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.repository.EventsSimilarityRepository;
import ru.practicum.ewm.repository.UserActionRepository;
import ru.practicum.ewm.stats.protobuf.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.protobuf.RecommendedEventProto;
import ru.practicum.ewm.stats.protobuf.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.protobuf.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationsHandlerImpl implements RecommendationsHandler {
    private final EventsSimilarityRepository eventsSimilarityRepository;
    private final UserActionRepository userActionRepository;
    private static final int MAX_LAST_VISITED_EVENTS_COUNT = 20; // Максимальное количество последних посещенных мероприятий
    private static final int MAX_SIMILAR_NEIGHBORS_COUNT = 3; // Максимальное количество соседей по подобию при расчете предсказанной оценки

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Sort sort = Sort.by("lastActionDate").descending();
        Pageable pageable = PageRequest.of(0, MAX_LAST_VISITED_EVENTS_COUNT, sort);
        long userId = request.getUserId();
        Set<Long> recentVisits = userActionRepository.findAllByUserId(userId, pageable).stream()
                .map(UserAction::getEventId).collect(Collectors.toSet());
        List<EventSimilarity> similarNeighborList = eventsSimilarityRepository
                .findAllByEventAInOrEventBIn(recentVisits, recentVisits, Sort.by("score").descending());
        Set<Long> unvisitedEventIds = getUnvisitedEvents(similarNeighborList, userId);
        Set<Long> recommendedEvents = new HashSet<>();
        for (EventSimilarity eventSimilarity : similarNeighborList) {
            if (recommendedEvents.size() == request.getMaxResults()) break;
            Long eventA = eventSimilarity.getEventA();
            if (unvisitedEventIds.contains(eventA)) {
                recommendedEvents.add(eventA);
                continue;
            }
            Long eventB = eventSimilarity.getEventB();
            if (unvisitedEventIds.contains(eventB)) {
                recommendedEvents.add(eventB);
            }
        }
        similarNeighborList = eventsSimilarityRepository.findAllByEventAInOrEventBIn(recommendedEvents, recommendedEvents,
                Sort.by("score").descending());
        Map<Long, Double> userActionsMap = getUserActionsForEvents(similarNeighborList, userId).stream()
                .collect(Collectors.toMap(UserAction::getEventId, UserAction::getWeight));
        Map<Long, Map<Long, EventSimilarity>> eventSimilarityForCalculate = new HashMap<>();
        int maxCount = recommendedEvents.size() * MAX_SIMILAR_NEIGHBORS_COUNT;
        int count = 0;
        for (EventSimilarity eventSimilarity : similarNeighborList) {
            if (count == maxCount) break;
            Long eventA = eventSimilarity.getEventA();
            Long eventB = eventSimilarity.getEventB();
            Long eventForCalculate = null;
            Map<Long, EventSimilarity> eventSimilarities = null;
            if (userActionsMap.containsKey(eventB)) {
                eventSimilarities = eventSimilarityForCalculate.computeIfAbsent(eventA, e -> new HashMap<>());
                eventForCalculate = eventB;
            }
            if (userActionsMap.containsKey(eventA)) {
                eventSimilarities = eventSimilarityForCalculate.computeIfAbsent(eventB, e -> new HashMap<>());
                eventForCalculate = eventA;
            }
            if (eventSimilarities != null && eventSimilarities.size() != MAX_SIMILAR_NEIGHBORS_COUNT) {
                eventSimilarities.put(eventForCalculate, eventSimilarity);
                count++;
            }
        }
        Map<Long, Double> predictedScore = getPredictedScore(eventSimilarityForCalculate, userActionsMap);
        return recommendedEvents.stream().map(eventId -> RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(predictedScore.get(eventId))
                .build()).sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed()).toList();
    }

    private Map<Long, Double> getPredictedScore(Map<Long, Map<Long, EventSimilarity>> eventSimilarityForCalculate, Map<Long, Double> userActionsMap) {
        Map<Long, Double> predictedScore = new HashMap<>();
        for (Map.Entry<Long, Map<Long, EventSimilarity>> entry : eventSimilarityForCalculate.entrySet()) {
            double weightedScoreSum = 0.0;
            double scoreSum = 0.0;
            for (Map.Entry<Long, EventSimilarity> similarityEntry : entry.getValue().entrySet()) {
                Double weight = userActionsMap.get(similarityEntry.getKey());
                Double score = similarityEntry.getValue().getScore();
                weightedScoreSum += weight * score;
                scoreSum += score;
            }
            predictedScore.put(entry.getKey(), weightedScoreSum / scoreSum);
        }
        return predictedScore;
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Sort sort = Sort.by("score").descending();
        long eventId = request.getEventId();
        List<EventSimilarity> eventSimilarityList = eventsSimilarityRepository
                .findAllByEventAOrEventB(eventId, eventId, sort);
        Set<Long> unvisitedEventIds = getUnvisitedEvents(eventSimilarityList, request.getUserId());
        return (eventSimilarityList.stream()
                .filter(eventSimilarity -> unvisitedEventIds.contains(eventSimilarity.getEventA())
                        || unvisitedEventIds.contains(eventSimilarity.getEventB())).map(eventSimilarity -> {
                            return RecommendedEventProto.newBuilder()
                                    .setEventId(eventSimilarity.getEventA() == eventId ? eventSimilarity.getEventB() : eventSimilarity.getEventA())
                                    .setScore(eventSimilarity.getScore())
                                    .build();
                        }
                ).limit(request.getMaxResults()).toList());
    }

    private Set<Long> getUnvisitedEvents(List<EventSimilarity> allEventSimilarity, Long userId) {
        Set<Long> allEventIds = allEventSimilarity.stream()
                .flatMap(eventSimilarity -> Stream.of(eventSimilarity.getEventA(), eventSimilarity.getEventB()))
                .collect(Collectors.toSet());
        Set<Long> visitedEventIds = userActionRepository.findAllByUserIdAndEventIdIn(userId, allEventIds)
                .stream().map(UserAction::getEventId).collect(Collectors.toSet());
        return allEventIds.stream().filter(id -> !visitedEventIds.contains(id)).collect(Collectors.toSet());
    }

    private List<UserAction> getUserActionsForEvents(List<EventSimilarity> allEventSimilarity, Long userId) {
        Set<Long> allEventIds = allEventSimilarity.stream()
                .flatMap(eventSimilarity -> Stream.of(eventSimilarity.getEventA(), eventSimilarity.getEventB()))
                .collect(Collectors.toSet());
        return userActionRepository.findAllByUserIdAndEventIdIn(userId, allEventIds);
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<WeightSum> weightSumList = userActionRepository.getWeightSumByEventIds(new HashSet<>(request.getEventIdList()));
        return weightSumList.stream().map(weightSum -> RecommendedEventProto.newBuilder()
                .setEventId(weightSum.getEventId())
                .setScore(weightSum.getWeightSum())
                .build()).toList();
    }
}
