package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@Setter
@Getter
@ToString
@ConfigurationProperties("aggregator.kafka")
public class KafkaClientProperties {
    private Properties producer;
    private ConsumerProperties consumer;

    @ToString
    @Setter
    @Getter
    public static class ConsumerProperties {
        private Properties base;
        private Long pollDurationSeconds;
    }
}
