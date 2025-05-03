package ru.practicum.ewm.partrequest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.UserServiceClient;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictDataException;
import ru.practicum.ewm.exception.DuplicateException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.partrequest.dto.ParticipationRequestDto;
import ru.practicum.ewm.partrequest.enums.Status;
import ru.practicum.ewm.partrequest.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.partrequest.model.ParticipationRequest;
import ru.practicum.ewm.partrequest.repository.ParticipationRequestRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final UserServiceClient userServiceClient;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        if (eventId == 0) {
            throw new ValidationException("Не задано id события");
        }

        userServiceClient.getUserById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c id: " + eventId + " не найдено"));
        if (requestRepository.existsByRequesterIdAndEvent(userId, event)) {
            throw new DuplicateException("Такой запрос уже существует");
        }
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictDataException("Пользователь не может создать запрос на участие в своем же событии");
        }
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictDataException("Нельзя участвовать в неопубликованном событии");
        }
        Integer participantLimit = event.getParticipantLimit();
        Integer confirmedRequests = event.getConfirmedRequests();
        if (!participantLimit.equals(0) && participantLimit.equals(confirmedRequests)) {
            throw new ConflictDataException("Лимит запросов на участие в событии уже достигнут");
        }
        Status status;
        if (participantLimit.equals(0) || !event.getRequestModeration()) {
            status = Status.CONFIRMED;
            event.setConfirmedRequests(++confirmedRequests);
        } else
            status = Status.PENDING;
        ParticipationRequest participationRequest = ParticipationRequest.builder()
                .requesterId(userId)
                .event(event)
                .status(status)
                .build();
        return ParticipationRequestMapper.toParticipationRequestDto(requestRepository.save(participationRequest));
    }

    @Transactional
    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userServiceClient.getUserById(userId);
        ParticipationRequest request = requestRepository.findByRequesterIdAndId(userId, requestId)
                .orElseThrow(() -> new NotFoundException("У пользователя с id: " + userId +
                        " не найдено запроса с id: " + requestId));
        if (request.getStatus() == Status.CONFIRMED) {
            Event event = request.getEvent();
            Integer confirmedRequests = event.getConfirmedRequests();
            event.setConfirmedRequests(--confirmedRequests);
        }
        request.setStatus(Status.CANCELED);
        return ParticipationRequestMapper.toParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getAllUserRequests(Long userId) {
        userServiceClient.getUserById(userId);
        return ParticipationRequestMapper.toParticipationRequestDto(requestRepository.findAllByRequesterId(userId));
    }

    public Map<Long, List<ParticipationRequest>> prepareConfirmedRequests(List<Long> eventIds) {
        log.info("Получаем список подтверждённых запросов для всех событий.");

        List<ParticipationRequest> confirmedRequests = requestRepository.findConfirmedRequests(eventIds);

        Map<Long, List<ParticipationRequest>> result = new HashMap<>();

        for (ParticipationRequest request : confirmedRequests) {
            var eventId = request.getEvent().getId();
            List<ParticipationRequest> list = result.get(eventId);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(request);
            result.put(eventId, list);
        }
        return result;
    }
}
