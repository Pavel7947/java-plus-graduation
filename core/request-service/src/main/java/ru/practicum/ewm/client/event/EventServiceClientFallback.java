package ru.practicum.ewm.client.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.exception.ServiceUnavailableException;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class EventServiceClientFallback implements EventServiceClient {
    private final Throwable throwable;

    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> events, List<Long> users,
                                                List<State> states, List<Long> categories,
                                                Boolean includeConfirmedRequests,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                Integer from, Integer size) {
        log.info("При получении списка событий возникло исключение: {}", throwable.getClass());
        throw new ServiceUnavailableException(throwable);
    }

    @Override
    public EventFullDto getEventById(Long eventId, Boolean includeConfirmedRequests, Boolean includeAuthorAdditionalInfo) {
        log.info("При получении события по id возникло исключение: {}", throwable.getClass());
        throw new ServiceUnavailableException(throwable);
    }

    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        throw new RuntimeException("Fallback метод не реализован");
    }
}
