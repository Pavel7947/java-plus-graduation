package ru.practicum.ewm.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Configuration
@RequiredArgsConstructor
public class KafkaClientConfig {
    private final KafkaClientProperties properties;

    @Bean
    public Producer<Long, SpecificRecordBase> getProducer() {
        return new KafkaProducer<>(properties.getProducer());
    }

    @Bean
    public Consumer<Long, UserActionAvro> getConsumer() {
        return new KafkaConsumer<>(properties.getConsumer().getBase());
    }
}
