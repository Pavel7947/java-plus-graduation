package ru.practicum.ewm.event.controller;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.api.EventServiceAdminResource;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.EventAdminFilter;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.InvalidDateTimeException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@Validated
public class AdminEventController implements EventServiceAdminResource {

    private final EventService eventService;

    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> events, List<Long> users, List<State> states,
                                                List<Long> categories, Boolean includeConfirmedRequests,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from,
                                                Integer size) {
        log.info("Получение полной информации обо всех событиях подходящих под переданные условия.");

        var filter = EventAdminFilter
                .builder()
                .users(users)
                .states(states)
                .categories(categories)
                .from(from)
                .size(size)
                .events(events)
                .includeConfirmedRequests(includeConfirmedRequests)
                .build();

        if (rangeStart != null && rangeEnd != null) {
            filter.setRangeStart(rangeStart);
            filter.setRangeEnd(rangeEnd);
            if (!filter.getRangeStart().isBefore(filter.getRangeEnd())) {
                throw new InvalidDateTimeException("Дата окончания события не может быть раньше даты начала события.");
            }
        }
        try {
            return eventService.getEventsForAdmin(filter);
        } catch (Exception e) {
            log.error("При запуске с параметрами {}", filter, e);
            throw e;
        }

    }

    @Override
    public EventFullDto updateEvent(@PositiveOrZero @PathVariable Long eventId,
                                    @Validated @RequestBody UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("Редактирование данных любого события администратором.");

        try {
            return eventService.updateEventAdmin(eventId, updateEventAdminRequest);
        } catch (Exception e) {
            log.error("При запуске updateEvent c id: {}, с параметрами {}", eventId, updateEventAdminRequest, e);
            throw e;
        }
    }

    @Override
    public EventFullDto getEventById(Long eventId, Boolean includeConfirmedRequests, Boolean includeAuthorAdditionalInfo) {
        log.info("Запрос на получение события по id");
        return eventService.getEventForAdmin(eventId, includeConfirmedRequests, includeAuthorAdditionalInfo);
    }
}
