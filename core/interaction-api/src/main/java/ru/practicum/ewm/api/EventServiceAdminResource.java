package ru.practicum.ewm.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.ewm.dto.DateTimeFormat.TIME_PATTERN;

public interface EventServiceAdminResource {

    @GetMapping("/admin/events")
    List<EventFullDto>
    getEventsForAdmin(@RequestParam(required = false) List<Long> events,
                      @RequestParam(required = false) List<Long> users,
                      @RequestParam(required = false) List<State> states,
                      @RequestParam(required = false) List<Long> categories,
                      @RequestParam(defaultValue = "true") Boolean includeConfirmedRequests,
                      @RequestParam(required = false) @DateTimeFormat(pattern = TIME_PATTERN) LocalDateTime rangeStart,
                      @RequestParam(required = false) @DateTimeFormat(pattern = TIME_PATTERN) LocalDateTime rangeEnd,
                      @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                      @Positive @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/admin/events/{eventId}")
    EventFullDto getEventById(@PathVariable @Positive Long eventId,
                              @RequestParam(defaultValue = "true") Boolean includeConfirmedRequests,
                              @RequestParam(defaultValue = "true") Boolean includeAuthorAdditionalInfo);

    @PatchMapping("/admin/events/{eventId}")
    EventFullDto updateEvent(@PositiveOrZero @PathVariable Long eventId,
                             @RequestBody UpdateEventAdminRequest updateEventAdminRequest);
}
