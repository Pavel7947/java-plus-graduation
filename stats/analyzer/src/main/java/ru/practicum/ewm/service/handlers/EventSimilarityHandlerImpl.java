package ru.practicum.ewm.service.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.model.EventSimilarityId;
import ru.practicum.ewm.repository.EventsSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSimilarityHandlerImpl implements EventSimilarityHandler {
    private final EventsSimilarityRepository repository;

    @Transactional
    @Override
    public void handle(EventSimilarityAvro eventSimilarityAvro) {
        log.debug("Поступило в обработку сообщение {}", eventSimilarityAvro);
        Optional<EventSimilarity> eventSimilarityOpt = repository.findById(EventSimilarityId.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .build());
        if (eventSimilarityOpt.isPresent()) {
            EventSimilarity eventSimilarity = eventSimilarityOpt.get();
            Instant similarityAvroTimestamp = eventSimilarityAvro.getTimestamp();
            if (!eventSimilarity.getActionDate().isBefore(similarityAvroTimestamp)) {
                log.trace("Поступившее собщение неактуально по дате");
                return;
            }
            eventSimilarity.setScore(eventSimilarityAvro.getScore());
            eventSimilarity.setActionDate(similarityAvroTimestamp);
            return;
        }
        log.trace("Сходство данных событий ранее в БД не сохранялось создаем новое");
        repository.save(EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .actionDate(eventSimilarityAvro.getTimestamp())
                .score(eventSimilarityAvro.getScore())
                .build());

    }
}
