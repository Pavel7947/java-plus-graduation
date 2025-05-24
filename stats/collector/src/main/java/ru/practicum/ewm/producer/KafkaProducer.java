package ru.practicum.ewm.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.config.KafkaProducerProperties;

import java.time.Instant;

@Component
@Slf4j
public class KafkaProducer {
    private final Producer<Long, SpecificRecordBase> producer;

    public KafkaProducer(KafkaProducerProperties config) {
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(config.getProducer());
    }

    public void send(String topic, Instant timestamp, Long eventId, SpecificRecordBase action) {
        ProducerRecord<Long, SpecificRecordBase> record =
                new ProducerRecord<>(topic, null, timestamp.toEpochMilli(), eventId, action);
        log.info("Сохраняю событие {}, в топик {}", action, topic);
        producer.send(record);
    }
}
