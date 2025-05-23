package ru.practicum.ewm.service.handlers;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionHandler {
    void handle(UserActionAvro userActionAvro);
}
