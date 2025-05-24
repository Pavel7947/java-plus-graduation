package ru.practicum.ewm.service.handlers;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityHandler {
    void handle(EventSimilarityAvro eventSimilarityAvro);
}
