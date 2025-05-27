package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.producer.KafkaProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.constants.StatsTopics;
import ru.practicum.ewm.stats.protobuf.ActionTypeProto;
import ru.practicum.ewm.stats.protobuf.UserActionProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserActionHandlerImpl implements UserActionHandler {
    private final KafkaProducer kafkaProducer;

    @Override
    public void collectUserAction(UserActionProto request) {
        UserActionAvro userActionAvro = mapToUserActionAvro(request);
        kafkaProducer.send(StatsTopics.STATS_USER_ACTIONS_V1_TOPIC, userActionAvro.getTimestamp(), userActionAvro.getEventId(), userActionAvro
        );
    }

    private UserActionAvro mapToUserActionAvro(UserActionProto userActionProto) {
        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setTimestamp(Instant.ofEpochSecond(userActionProto.getTimestamp().getSeconds(),
                        userActionProto.getTimestamp().getNanos()))
                .setActionType(mapToActionTypeAvro(userActionProto.getActionType()))
                .build();
    }

    private ActionTypeAvro mapToActionTypeAvro(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;

            case UNRECOGNIZED -> throw new IllegalArgumentException("Неизвестная константа перечисления: " +
                    ActionTypeProto.class.getSimpleName());
        };
    }
}
