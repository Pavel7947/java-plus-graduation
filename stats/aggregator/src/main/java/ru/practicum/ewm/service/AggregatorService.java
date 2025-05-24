package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.config.KafkaClientProperties;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.constants.StatsTopics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatorService implements CommandLineRunner {
    private final Consumer<Long, UserActionAvro> consumer;
    private final Producer<Long, SpecificRecordBase> producer;
    private final Map<Long, Map<Long, Double>> userActionWeightMap = new HashMap<>();
    private final Map<Long, Double> weightSumMap = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSumMap = new HashMap<>();
    private final Map<TopicPartition, OffsetAndMetadata> currentOffset = new HashMap<>();
    private final KafkaClientProperties kafkaClientProperties;

    @Override
    public void run(String... args) {
        try {
            consumer.subscribe(List.of(StatsTopics.STATS_USER_ACTIONS_V1_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(Duration.ofSeconds(kafkaClientProperties.getConsumer().getPollDurationSeconds()));
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    UserActionAvro userActionAvro = record.value();
                    long eventId = userActionAvro.getEventId();
                    if (!userActionWeightMap.containsKey(eventId)) {
                        calculateSimilarityForNewEvent(userActionAvro).forEach(this::send);
                    } else {
                        calculateSimilarityForExistingEvent(userActionAvro).forEach(this::send);
                    }
                    currentOffset.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }
                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Во время фиксации произошла ошибка. Офсет: {}", offsets, exception);
                    }
                });
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                consumer.commitSync(currentOffset);
                producer.flush();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private void send(EventSimilarityAvro eventSimilarityAvro) {
        Long id = eventSimilarityAvro.getEventA();
        Long timestamp = eventSimilarityAvro.getTimestamp().toEpochMilli();
        String topic = StatsTopics.STATS_EVENT_SIMILARITY_V1_TOPIC;
        ProducerRecord<Long, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(topic, null, timestamp, id, eventSimilarityAvro);
        log.info("Сохраняю запись {}, в топик {}", eventSimilarityAvro, topic);
        producer.send(producerRecord);
    }

    /**
     * Расчитывает коэффицент подобия с остальными событиями для нового события. Также обновляет состояние коллекций
     *
     * @param userActionAvro Запись c событием UserActionAvro
     * @return Пустой лист - если переданное событие никак не повлияло на коэффициент подобия с другими событиями.
     * В обратном случае возвращается лист обьектов {@code EventSimilarityAvro} в которых id событий упорядочены по возрастанию.
     */
    private List<EventSimilarityAvro> calculateSimilarityForNewEvent(UserActionAvro userActionAvro) {
        long userId = userActionAvro.getUserId();
        long eventId = userActionAvro.getEventId();
        double weight = getWeightByAction(userActionAvro);
        userActionWeightMap.computeIfAbsent(eventId, e -> new HashMap<>()).put(userId, weight);
        List<EventSimilarityAvro> similarityAvroList = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : weightSumMap.entrySet()) {
            long first = Math.min(eventId, entry.getKey());
            long second = Math.max(eventId, entry.getKey());
            double minWeightsSum = Math.min(userActionWeightMap.get(entry.getKey()).getOrDefault(userId, 0.0), weight);
            Map<Long, Double> minWeightSumMapForFirstEvent = minWeightsSumMap.computeIfAbsent(first, e -> new HashMap<>());
            if (minWeightsSum != 0.0) {
                minWeightSumMapForFirstEvent.put(second, minWeightsSum);
                double score = minWeightsSum / (Math.sqrt(entry.getValue()) * Math.sqrt(weight));
                similarityAvroList.add(EventSimilarityAvro.newBuilder()
                        .setEventA(first)
                        .setEventB(second)
                        .setScore(score)
                        .setTimestamp(userActionAvro.getTimestamp())
                        .build());
            }
        }
        weightSumMap.put(eventId, weight);
        return similarityAvroList;
    }


    /**
     * Расчитывает коэффицент подобия с остальными событиями для существующего события. Также обновляет состояние коллекций
     *
     * @param userActionAvro Запись c событием UserActionAvro
     * @return Пустой лист - если переданное событие никак не повлияло на коэффициент подобия с другими событиями.
     * В обратном случае возвращается лист обьектов {@code EventSimilarityAvro} в которых id событий упорядочены по возрастанию.
     */
    private List<EventSimilarityAvro> calculateSimilarityForExistingEvent(UserActionAvro userActionAvro) {
        long eventA = userActionAvro.getEventId();
        Map<Long, Double> usersWeight = userActionWeightMap.get(eventA);
        long userId = userActionAvro.getUserId();
        double oldWeight = usersWeight.getOrDefault(userId, 0.0);
        double newWeight = getWeightByAction(userActionAvro);
        if (oldWeight >= newWeight) {
            return List.of();
        }
        usersWeight.put(userId, newWeight);
        weightSumMap.computeIfPresent(eventA, (key, oldValue) -> oldValue + (newWeight - oldWeight));
        List<EventSimilarityAvro> similarityAvroList = new ArrayList<>();
        for (Long eventB : userActionWeightMap.keySet()) {
            if (eventA == eventB) continue;
            double eventBUserWeight = userActionWeightMap.get(eventB).getOrDefault(userId, 0.0);
            if (eventBUserWeight == 0.0) continue;
            long first = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);
            double delta = Math.min(newWeight, eventBUserWeight) - Math.min(oldWeight, eventBUserWeight);
            Map<Long, Double> minWeightSumMapForFirstEvent = minWeightsSumMap.get(first);
            minWeightSumMapForFirstEvent.put(second, minWeightSumMapForFirstEvent.getOrDefault(second, 0.0) + delta);
            double score = minWeightSumMapForFirstEvent.get(second) / (Math.sqrt(weightSumMap.get(first)) * Math.sqrt(weightSumMap.get(second)));
            similarityAvroList.add(EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(score)
                    .setTimestamp(userActionAvro.getTimestamp())
                    .build());
        }
        return similarityAvroList;
    }

    private double getWeightByAction(UserActionAvro userActionAvro) {
        return switch (userActionAvro.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1;
        };
    }
}
