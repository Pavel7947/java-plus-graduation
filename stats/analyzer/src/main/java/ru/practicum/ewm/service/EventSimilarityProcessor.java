package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.config.KafkaConsumerProperties;
import ru.practicum.ewm.service.handlers.EventSimilarityHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.constants.StatsTopics;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSimilarityProcessor implements Runnable {
    private final Consumer<Long, EventSimilarityAvro> consumer;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffset = new HashMap<>();
    private final EventSimilarityHandler eventSimilarityHandler;
    private final KafkaConsumerProperties properties;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(StatsTopics.STATS_EVENT_SIMILARITY_V1_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records =
                        consumer.poll(Duration.ofSeconds(properties.getPollDurationSeconds().getEvent_similarity()));
                if (!records.isEmpty()) {
                    log.info("Поступили в обработку записи кол-во: {}", records.count());
                }
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    eventSimilarityHandler.handle(record.value());
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
            log.error("Ошибка во время обработки событий от хабов", e);
        } finally {
            try {
                consumer.commitSync(currentOffset);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }
}
