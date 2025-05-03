package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.partrequest.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(NewEventDto eventDto, Long userId);

    List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size);

    EventFullDto getEventOfUser(Long userId, Long eventId);

    EventFullDto updateEventOfUser(UpdateEventUserRequest updateRequest, Long userId, Long eventId);

    List<EventShortDto> getPublicEventsByFilter(HttpServletRequest httpServletRequest, EventPublicFilter inputFilter);

    EventFullDto getPublicEventById(HttpServletRequest httpServletRequest, Long id);

    List<EventFullDto> getEventsForAdmin(EventAdminFilter admin);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<ParticipationRequestDto> getRequestsOfUserEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestsStatus(EventRequestStatusUpdateRequest updateRequest, Long userId,
                                                        Long eventId);
}
