package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.EventAdminFilter;
import ru.practicum.ewm.event.dto.EventPublicFilter;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(NewEventDto eventDto, Long userId);

    List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size);

    EventFullDto getEventOfUser(Long userId, Long eventId);

    EventFullDto updateEventOfUser(UpdateEventUserRequest updateRequest, Long userId, Long eventId);

    List<EventShortDto> getPublicEventsByFilter(HttpServletRequest httpServletRequest, EventPublicFilter inputFilter);

    EventFullDto getPublicEventById(HttpServletRequest httpServletRequest, Long id);

    List<EventFullDto> getEventsForAdmin(EventAdminFilter admin);

    EventFullDto getEventForAdmin(Long eventId, Boolean includeConfirmedRequests);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);
}
