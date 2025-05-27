package ru.practicum.ewm.service;

import ru.practicum.ewm.stats.protobuf.UserActionProto;

public interface UserActionHandler {

    void collectUserAction(UserActionProto request);
}
