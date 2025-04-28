package ru.practicum.ewm.partrequest.service;

import ru.practicum.ewm.partrequest.dto.ParticipationRequestDto;
import ru.practicum.ewm.partrequest.model.ParticipationRequest;

import java.util.List;
import java.util.Map;

public interface ParticipationRequestService {

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getAllUserRequests(Long userId);

    Map<Long, List<ParticipationRequest>> prepareConfirmedRequests(List<Long> eventIds);
}
