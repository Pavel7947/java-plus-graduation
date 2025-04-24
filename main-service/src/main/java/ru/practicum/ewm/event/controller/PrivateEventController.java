package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.partrequest.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
@Slf4j
public class PrivateEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEventsOfUser(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "0") Integer from,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return eventService.getEventsOfUser(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@Valid @RequestBody NewEventDto eventDto, @PathVariable Long userId) {
        log.info("Получили запрос на создание нового события");
        return eventService.addEvent(eventDto, userId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventOfUser(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getEventOfUser(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventOfUser(@Valid @RequestBody UpdateEventUserRequest updateRequest,
                                          @PathVariable Long userId,
                                          @PathVariable Long eventId) {
        return eventService.updateEventOfUser(updateRequest, userId, eventId);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsOfUserEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getRequestsOfUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestsStatus(@PathVariable Long userId, @PathVariable Long eventId,
                                                               @Valid @RequestBody
                                                               EventRequestStatusUpdateRequest updateRequest) {
        log.info("Получили запрос на обновление статусов заявок");
        return eventService.updateRequestsStatus(updateRequest, userId, eventId);
    }
}
