package ru.practicum.ewm.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.request.RequestDto;

import java.util.List;

@Component
@Slf4j
public class RequestServiceClientFallback implements RequestServiceClient {

    @Override
    public List<RequestDto> getAllRequests(List<Long> eventIds, Boolean confirmed, Integer from, Integer size) {
        log.info("Неудалось получить данные от request-service. Вернул пустой список");
        return List.of();
    }
}
