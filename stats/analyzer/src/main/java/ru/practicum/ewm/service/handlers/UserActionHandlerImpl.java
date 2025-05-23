package ru.practicum.ewm.service.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.model.UserActionId;
import ru.practicum.ewm.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionHandlerImpl implements UserActionHandler {
    private final UserActionRepository userActionRepository;

    @Transactional
    @Override
    public void handle(UserActionAvro userActionAvro) {
        log.debug("Поступило в обработку событие {}", userActionAvro);
        Optional<UserAction> userActionOpt = userActionRepository.findById(UserActionId.builder()
                .eventId(userActionAvro.getEventId())
                .userId(userActionAvro.getUserId())
                .build());
        double newWeight = getWeightByAction(userActionAvro);
        Instant newTimestamp = userActionAvro.getTimestamp();
        if (userActionOpt.isPresent()) {
            UserAction userAction = userActionOpt.get();
            if (userAction.getWeight() < newWeight) {
                userAction.setWeight(newWeight);
            }
            if (userAction.getLastActionDate().isBefore(newTimestamp)) {
                userAction.setLastActionDate(newTimestamp);
            }
            return;
        }
        userActionRepository.save(UserAction.builder()
                .userId(userActionAvro.getUserId())
                .eventId(userActionAvro.getEventId())
                .lastActionDate(newTimestamp)
                .weight(newWeight)
                .build());
    }

    private double getWeightByAction(UserActionAvro userActionAvro) {
        return switch (userActionAvro.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1;
        };
    }
}
