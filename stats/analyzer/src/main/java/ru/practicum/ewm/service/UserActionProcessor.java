package ru.practicum.ewm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.config.KafkaConsumerProperties;
import ru.practicum.ewm.service.handlers.UserActionHandler;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.constants.StatsTopics;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserActionProcessor implements Runnable {
    private final Consumer<Long, UserActionAvro> consumer;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffset = new HashMap<>();
    private final UserActionHandler userActionHandler;
    private final KafkaConsumerProperties properties;

    public UserActionProcessor(Consumer<Long, UserActionAvro> consumer,
                               UserActionHandler userActionHandler,
                               KafkaConsumerProperties properties) {
        this.consumer = consumer;
        this.userActionHandler = userActionHandler;
        this.properties = properties;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(StatsTopics.STATS_USER_ACTIONS_V1_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(Duration.ofSeconds(properties.getPollDurationSeconds().getUserAction()));
                if (!records.isEmpty()) {
                    log.info("Поступили в обработку записи кол-во: {}", records.count());
                }
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    userActionHandler.handle(record.value());
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
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }
}
