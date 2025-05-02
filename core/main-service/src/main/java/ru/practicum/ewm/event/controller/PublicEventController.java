package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventPublicFilter;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.enums.SortType;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.InvalidDateTimeException;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.ewm.utils.date.DateTimeFormat.TIME_PATTERN;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto>
    getEventsByFilter(HttpServletRequest httpServletRequest,
                      @RequestParam(name = "text", defaultValue = "") String text,
                      @RequestParam(name = "categories", required = false) List<Long> categories,
                      @RequestParam(name = "paid", required = false) Boolean paid,
                      @RequestParam(required = false) @DateTimeFormat(pattern = TIME_PATTERN) LocalDateTime rangeStart,
                      @RequestParam(required = false) @DateTimeFormat(pattern = TIME_PATTERN) LocalDateTime rangeEnd,
                      @RequestParam(name = "onlyAvailable", defaultValue = "false") Boolean onlyAvailable,
                      @RequestParam(name = "sort", defaultValue = "EVENT_DATE") SortType sort,
                      @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero Integer from,
                      @RequestParam(name = "size", defaultValue = "10") @Positive Integer size) {
        log.info("Получение событий с возможностью фильтрации. GET /events text:{}, categories:{}, paid:{}," +
                        " rangeStart:{}, rangeEnd:{}, onlyAvailable:{}, sort:{}, from:{}, size:{}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        var filter = new EventPublicFilter();
        if (rangeStart != null && rangeEnd != null) {
            filter.setRangeStart(rangeStart);
            filter.setRangeEnd(rangeEnd);
            if (!filter.getRangeStart().isBefore(filter.getRangeEnd())) {
                throw new InvalidDateTimeException("Дата окончания события не может быть раньше даты начала события.");
            }
        }
        filter.setSort(sort);
        filter.setFrom(from);
        filter.setSize(size);
        filter.setText(text);
        filter.setCategories(categories);
        filter.setPaid(paid);
        filter.setOnlyAvailable(onlyAvailable);

        try {
            return eventService.getPublicEventsByFilter(httpServletRequest, filter);
        } catch (Exception e) {
            log.error("При запуске с параметрами " + filter, e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(HttpServletRequest httpServletRequest, @PathVariable("id") @Positive Long id) {
        log.info("Получение подробной информации об опубликованном событии по его идентификатору.");

        try {
            return eventService.getPublicEventById(httpServletRequest, id);
        } catch (Exception e) {
            log.error("При запуске с параметрами id " + id, e);
            throw e;
        }
    }
}
