package ru.practicum.ewm.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.EventServiceClient;
import ru.practicum.ewm.client.UserServiceClient;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.RequestSearchFilter;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.dto.request.Status;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.QRequest;
import ru.practicum.ewm.model.Request;
import ru.practicum.ewm.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;

    @Transactional
    @Override
    public RequestDto addRequest(Long userId, Long eventId) {
        if (eventId == 0) {
            throw new ValidationException("Не задано id события");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new DuplicateException("Такой запрос уже существует");
        }
        userServiceClient.getUserById(userId);
        EventFullDto event = eventServiceClient.getEventById(eventId, false);
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictDataException("Пользователь не может создать запрос на участие в своем же событии");
        }
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictDataException("Нельзя участвовать в неопубликованном событии");
        }
        Integer participantLimit = event.getParticipantLimit();
        Integer confirmedRequests = requestRepository.findAllByStatusAndEventId(Status.CONFIRMED, eventId).size();
        if (!participantLimit.equals(0) && participantLimit.equals(confirmedRequests)) {
            throw new ConflictDataException("Лимит запросов на участие в событии уже достигнут");
        }
        Status status;
        if (participantLimit.equals(0) || !event.getRequestModeration()) {
            status = Status.CONFIRMED;
        } else
            status = Status.PENDING;
        Request request = Request.builder()
                .requesterId(userId)
                .eventId(eventId)
                .status(status)
                .build();
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Transactional
    @Override
    public RequestDto cancelRequest(Long userId, Long requestId) {
        userServiceClient.getUserById(userId);
        Request request = requestRepository.findByRequesterIdAndId(userId, requestId)
                .orElseThrow(() -> new NotFoundException("У пользователя с id: " + userId +
                        " не найдено запроса с id: " + requestId));
        request.setStatus(Status.CANCELED);
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    public List<RequestDto> getAllUserRequests(Long userId) {
        userServiceClient.getUserById(userId);
        return RequestMapper.toParticipationRequestDto(requestRepository.findAllByRequesterId(userId));
    }

    @Override
    public List<RequestDto> getAllRequests(RequestSearchFilter filter) {
        Pageable pageable = PageRequest.of(filter.getFrom() / filter.getSize(), filter.getSize());
        BooleanExpression conditions = Expressions.TRUE;
        if (filter.getConfirmed() != null && filter.getConfirmed()) {
            conditions = conditions.and(QRequest.request.status.eq(Status.CONFIRMED));
        }
        if (filter.getEventIds() != null) {
            conditions = conditions.and(QRequest.request.eventId.in(filter.getEventIds()));
        }
        return RequestMapper
                .toParticipationRequestDto(requestRepository.findAll(conditions, pageable).getContent());
    }

    @Override
    public List<RequestDto> getRequestsOfUserEvent(Long userId, Long eventId) {
        userServiceClient.getUserById(userId);
        EventFullDto event = eventServiceClient.getEventById(eventId, false);
        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            log.error("userId отличается от id создателя события");
            throw new ValidationException("Событие должно быть создано текущим пользователем");
        }
        return RequestMapper
                .toParticipationRequestDto(requestRepository.findAllByEventId(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                               EventRequestStatusUpdateRequest updateRequest) {
        userServiceClient.getUserById(userId);
        EventFullDto event = eventServiceClient.getEventById(eventId, false);
        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ValidationException("Событие должно быть создано текущим пользователем");
        }
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new BadRequestException("Подтверждение заявок для данного события не требуется");
        }
        int confirmedRequestsCount = requestRepository.findAllByStatusAndEventId(Status.CONFIRMED, eventId).size();
        if (Objects.equals(confirmedRequestsCount, event.getParticipantLimit())) {
            throw new ConflictDataException("Лимит участников уже исчерпан");
        }
        List<Long> requestIds = updateRequest.getRequestIds();
        log.info("Получили список id запросов на участие: {}", requestIds);
        List<Request> requestList = requestRepository.findAllById(requestIds);
        if (requestList.stream().anyMatch(request -> !Objects.equals(request.getEventId(), eventId))) {
            throw new ValidationException("Все запросы должны принадлежать одному событию");
        }
        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();
        switch (updateRequest.getStatus()) {
            case CONFIRMED -> {
                for (Request request : requestList) {
                    if (request.getStatus() != Status.PENDING) {
                        throw new ConflictDataException("Можно изменить только статус PENDING");
                    }
                    if (Objects.equals(confirmedRequestsCount, event.getParticipantLimit())) {
                        request.setStatus(Status.REJECTED);
                        rejectedRequests.add(request);
                    } else {
                        request.setStatus(Status.CONFIRMED);
                        confirmedRequestsCount++;
                        confirmedRequests.add(request);
                    }
                }
            }
            case REJECTED -> requestList.forEach(request -> {
                if (request.getStatus() != Status.PENDING) {
                    throw new ConflictDataException("Можно изменить только статус PENDING");
                }
                request.setStatus(Status.REJECTED);
                rejectedRequests.add(request);
            });
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(RequestMapper::toParticipationRequestDto).toList())
                .rejectedRequests(rejectedRequests.stream()
                        .map(RequestMapper::toParticipationRequestDto).toList())
                .build();
    }
}
