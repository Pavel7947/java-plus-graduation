package ru.practicum.ewm.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.client.request.RequestServiceClient;
import ru.practicum.ewm.client.user.UserServiceClient;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.event.dto.EventAdminFilter;
import ru.practicum.ewm.event.dto.EventPublicFilter;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.QEvent;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.stats.client.StatClient;
import ru.practicum.ewm.stats.client.UserActionType;
import ru.practicum.ewm.stats.protobuf.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final CategoryRepository categoryRepository;

    private final UserServiceClient userServiceClient;

    private final RequestServiceClient requestClient;

    private final StatClient statClient;

    @Value("${ewm.service.name}")
    private String serviceName;

    @Override
    @Transactional
    public EventFullDto addEvent(NewEventDto eventDto, Long userId) {
        checkFields(eventDto);
        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));
        UserDto initiator = userServiceClient.getUserById(userId);
        if (eventDto.getCommenting() == null) {
            eventDto.setCommenting(true);
        }
        Event event = eventRepository.save(EventMapper.mapToEvent(eventDto, category, initiator.getId()));
        return EventMapper.mapToFullDto(event, 0.0, UserMapper.mapToUserShort(initiator), 0);
    }

    @Override
    public List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size) {
        UserDto initiator = userServiceClient.getUserById(userId);
        Sort sortByCreatedDate = Sort.by("createdOn");
        PageRequest pageRequest = PageRequest.of(from / size, size, sortByCreatedDate);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageRequest);
        if (events.isEmpty()) {
            return List.of();
        }
        return EventMapper.mapToShortDto(events, List.of(initiator), getRating(events), getConfirmedRequests(events));
    }

    @Override
    public EventFullDto getEventOfUser(Long userId, Long eventId) {
        UserDto initiator = userServiceClient.getUserById(userId);
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Можно просмотреть только своё событие");
        }
        Optional<RecommendedEventProto> ratingOpt = getRating(List.of(event)).stream().findFirst();
        double rating = ratingOpt.map(RecommendedEventProto::getScore).orElse(0.0);
        int confirmedRequestCount = getConfirmedRequests(List.of(event)).size();
        return EventMapper.mapToFullDto(event, rating, UserMapper.mapToUserShort(initiator),
                confirmedRequestCount);
    }

    @Override
    @Transactional
    public EventFullDto updateEventOfUser(UpdateEventUserRequest updateRequest, Long userId, Long eventId) {
        UserDto initiator = userServiceClient.getUserById(userId);
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Можно редактировать только своё событие");
        }

        if (event.getState() == State.PUBLISHED) {
            throw new ConflictDataException("Нельзя изменить опубликованное событие");
        }

        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата начала события должна быть позже чем через 2 часа от текущего" +
                        " времени");
            }
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.setLat(updateRequest.getLocation().getLat());
            event.setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getCommenting() != null) {
            event.setCommenting(updateRequest.getCommenting());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(State.PENDING);
            } else if (updateRequest.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(State.CANCELED);
            }
        }
        int confirmedRequestCount = getConfirmedRequests(List.of(event)).size();
        return EventMapper.mapToFullDto(event, 0.0, UserMapper.mapToUserShort(initiator),
                confirmedRequestCount);
    }

    //public Получение событий с возможностью фильтрации
    @Override
    public List<EventShortDto> getPublicEventsByFilter(HttpServletRequest httpServletRequest,
                                                       EventPublicFilter inputFilter) {
        PageRequest pageRequest = PageRequest.of(inputFilter.getFrom() / inputFilter.getSize(),
                inputFilter.getSize());

        inputFilter.setText("%" + inputFilter.getText().trim() + "%");

        BooleanExpression conditions = QEvent.event.annotation.likeIgnoreCase(inputFilter.getText())
                .or(QEvent.event.description.likeIgnoreCase(inputFilter.getText()))
                .and(QEvent.event.state.in(State.PUBLISHED));
        if (inputFilter.getCategories() != null) {
            conditions = conditions.and(QEvent.event.category.id.in(inputFilter.getCategories()));
        }
        if (inputFilter.getPaid() != null) {
            conditions.and(QEvent.event.paid.eq(inputFilter.getPaid()));
        }
        if (inputFilter.getRangeStart() != null && inputFilter.getRangeEnd() != null) {
            conditions = conditions.and(QEvent.event.eventDate.after(inputFilter.getRangeStart()))
                    .and(QEvent.event.eventDate.before(inputFilter.getRangeEnd()));
        } else {
            conditions = conditions.and(QEvent.event.eventDate.after(LocalDateTime.now()));
        }
        List<Event> events = eventRepository.findAll(conditions, pageRequest).getContent();

        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Integer> confirmedRequestsCountMap = getConfirmedRequests(events)
                .stream().collect(Collectors.groupingBy(RequestDto::getEvent, Collectors.reducing(0, e -> 1, Integer::sum)));
        if (inputFilter.getOnlyAvailable()) {
            if (!confirmedRequestsCountMap.isEmpty()) {
                events = events.stream()
                        .filter(event -> confirmedRequestsCountMap.get(event.getId()) < event.getParticipantLimit()).toList();
            } else {
                log.debug("Запрос отклонен так как нет необходимых данных о подтвержденных заявках");
                throw new ServiceUnavailableException();
            }
        }
        List<EventShortDto> result = EventMapper.mapToShortDto(events, getInitiators(events),
                getRating(events), confirmedRequestsCountMap);
        List<EventShortDto> resultList = new ArrayList<>(result);

        switch (inputFilter.getSort()) {
            case EVENT_DATE -> resultList.sort(Comparator.comparing(EventShortDto::getEventDate));
            case RATING -> resultList.sort(Comparator.comparing(EventShortDto::getRating).reversed());
        }
        return resultList;
    }

    //public Получение подробной информации об опубликованном событии по его идентификатору
    @Override
    public EventFullDto getPublicEventById(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundRecordInBDException(String.format("Не найдено событие в БД с ID = %d.", eventId)));
        if (event.getState() != State.PUBLISHED)
            throw new NotFoundException("Посмотреть можно только опубликованное событие.");
        Optional<RecommendedEventProto> rating = getRating(List.of(event)).stream().findFirst();
        UserDto initiator = userServiceClient.getUserById(event.getInitiatorId());
        int confirmedRequestCount = getConfirmedRequests(List.of(event)).size();
        EventFullDto result = EventMapper.mapToFullDto(event, rating.map(RecommendedEventProto::getScore).orElse(0.0),
                UserMapper.mapToUserShort(initiator), confirmedRequestCount);
        try {
            statClient.collectUserAction(userId, eventId, UserActionType.VIEW);
            log.info("Передали информацию просмотре пользователем: {} события: {} в сервис рекомендаций", userId, eventId);
        } catch (SaveStatsException e) {
            log.error("Не удалось передать информацию о просмотре пользователем: {} события: {}", userId, eventId, e);
        }
        return result;
    }

    // admin Эндпоинт возвращает полную информацию обо всех событиях подходящих под переданные условия
    @Override
    public List<EventFullDto> getEventsForAdmin(EventAdminFilter input) {
        Sort sort = Sort.by("createdOn");
        Pageable pageable = PageRequest.of(input.getFrom() / input.getSize(), input.getSize(), sort);
        BooleanExpression conditions;
        if (input.getRangeStart() == null && input.getRangeEnd() == null) {
            conditions = QEvent.event.eventDate.after(LocalDateTime.now());
        } else {
            conditions = QEvent.event.eventDate.after(input.getRangeStart())
                    .and(QEvent.event.eventDate.before(input.getRangeEnd()));
        }
        if (input.getUsers() != null) {
            conditions = conditions.and(QEvent.event.initiatorId.in(input.getUsers()));
        }
        if (input.getEvents() != null) {
            conditions = conditions.and(QEvent.event.id.in(input.getEvents()));
        }
        if (input.getStates() != null) {
            conditions = conditions.and(QEvent.event.state.in(input.getStates()));
        }
        if (input.getCategories() != null) {
            conditions = conditions.and(QEvent.event.category.id.in(input.getCategories()));
        }
        List<Event> events = eventRepository.findAll(conditions, pageable).getContent();
        if (events.isEmpty()) {
            return List.of();
        }
        List<RequestDto> confirmedRequests;
        if (input.getIncludeConfirmedRequests()) {
            confirmedRequests = getConfirmedRequests(events);
        } else {
            confirmedRequests = List.of();
        }
        return EventMapper.mapToFullDto(events, getInitiators(events), getRating(events), confirmedRequests);
    }


    // admin Редактирование данных любого события администратором. Валидация данных не требуется
    @Transactional
    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundRecordInBDException(String.format("Не найдено событие в БД с ID = %d.", eventId)));

        checkStateAction(event, updateEventAdminRequest);

        if (updateEventAdminRequest.getAnnotation() != null && !updateEventAdminRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateEventAdminRequest.getAnnotation());
        }
        if (updateEventAdminRequest.getCategory() != null) {
            event.getCategory().setId(updateEventAdminRequest.getCategory());
        }
        if (updateEventAdminRequest.getDescription() != null && !updateEventAdminRequest.getDescription().isBlank()) {
            event.setDescription(updateEventAdminRequest.getDescription());
        }
        if (updateEventAdminRequest.getEventDate() != null) {
            checkDateEvent(updateEventAdminRequest.getEventDate());
            event.setEventDate(updateEventAdminRequest.getEventDate());
        }
        if (updateEventAdminRequest.getLocation() != null) {
            event.setLat(updateEventAdminRequest.getLocation().getLat());
            event.setLon(updateEventAdminRequest.getLocation().getLon());
        }
        if (updateEventAdminRequest.getPaid() != null) {
            event.setPaid(updateEventAdminRequest.getPaid());
        }
        if (updateEventAdminRequest.getCommenting() != null) {
            event.setCommenting(updateEventAdminRequest.getCommenting());
        }
        if (updateEventAdminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
        }
        if (updateEventAdminRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventAdminRequest.getRequestModeration());
        }
        if (StateAction.REJECT_EVENT.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.CANCELED);
        }
        if (StateAction.CANCEL_REVIEW.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.CANCELED);
        }
        if (StateAction.SEND_TO_REVIEW.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.PENDING);
        }
        if (StateAction.PUBLISH_EVENT.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }
        if (updateEventAdminRequest.getTitle() != null && !updateEventAdminRequest.getTitle().isBlank()) {
            event.setTitle(updateEventAdminRequest.getTitle());
        }
        event = eventRepository.save(event);

        Optional<RecommendedEventProto> rating = getRating(List.of(event)).stream().findFirst();
        UserDto initiator = userServiceClient.getUserById(event.getInitiatorId());
        int confirmedRequestCount = getConfirmedRequests(List.of(event)).size();
        return EventMapper.mapToFullDto(event, rating.map(RecommendedEventProto::getScore).orElse(0.0),
                UserMapper.mapToUserShort(initiator), confirmedRequestCount);
    }

    @Override
    public EventFullDto getEventForAdmin(Long eventId, Boolean includeConfirmedRequests, Boolean includeAuthorAdditionalInfo) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundRecordInBDException(String.format("Не найдено событие в БД с ID = %d.", eventId)));
        int confirmedRequestCount = 0;
        if (includeConfirmedRequests) {
            confirmedRequestCount = getConfirmedRequests(List.of(event)).size();
        }
        UserDto initiator;
        if (includeAuthorAdditionalInfo) {
            initiator = userServiceClient.getUserById(event.getInitiatorId());
        } else {
            initiator = UserDto.builder().id(event.getInitiatorId()).build();
        }
        Optional<RecommendedEventProto> rating = getRating(List.of(event)).stream().findFirst();
        return EventMapper.mapToFullDto(event, rating.map(RecommendedEventProto::getScore).orElse(0.0),
                UserMapper.mapToUserShort(initiator), confirmedRequestCount);
    }

    @Override
    public List<EventShortDto> getRecommendedEvents(Long userId, Integer maxResult) {
        List<Long> eventIds = statClient.getRecommendationsForUser(userId, maxResult)
                .map(RecommendedEventProto::getEventId).toList();
        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        return EventMapper.mapToShortDto(events, getInitiators(events), getRating(events), getConfirmedRequests(events));
    }

    @Override
    public EventShortDto likeEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundRecordInBDException(String.format("Не найдено событие в БД с ID = %d.", eventId)));
        List<RequestDto> confirmedRequests = getConfirmedRequests(List.of(event));
        boolean isParticipant = confirmedRequests.stream().anyMatch(requestDto -> requestDto.getRequester().equals(userId));
        ;
        if (!isParticipant) {
            throw new BadRequestException("Пользователь не может лайкать событие в котором не учавствовал");
        }
        statClient.collectUserAction(userId, eventId, UserActionType.LIKE);
        Optional<RecommendedEventProto> rating = getRating(List.of(event)).stream().findFirst();
        UserDto initiator = userServiceClient.getUserById(event.getInitiatorId());
        int confirmedRequestCount = confirmedRequests.size();
        return EventMapper.mapToShortDto(event, rating.map(RecommendedEventProto::getScore).orElse(0.0),
                UserMapper.mapToUserShort(initiator), confirmedRequestCount);
    }

    private void checkFields(NewEventDto dto) {
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата начала события должна быть позже чем через 2 часа от текущего времени");
        }

        if (dto.getPaid() == null) {
            dto.setPaid(false);
        }
        if (dto.getRequestModeration() == null) {
            dto.setRequestModeration(true);
        }
        if (dto.getParticipantLimit() == null) {
            dto.setParticipantLimit(0);
        }
    }

    private void checkDateEvent(LocalDateTime newDateTime) {
        LocalDateTime now = LocalDateTime.now().plusHours(1);
        if (now.isAfter(newDateTime)) {
            throw new InvalidDateTimeException(String.format("Дата начала события должна быть позже текущего времени на %s ч.", 1));
        }
    }

    private void checkStateAction(Event oldEvent, UpdateEventAdminRequest newEvent) {
        if (newEvent.getStateAction() == StateAction.PUBLISH_EVENT) {
            if (oldEvent.getState() != State.PENDING) {
                throw new OperationFailedException("Невозможно опубликовать событие. Его можно " +
                        "опубликовать только в состоянии ожидания публикации.");
            }
        }
        if (newEvent.getStateAction() == StateAction.REJECT_EVENT) {
            if (oldEvent.getState() == State.PUBLISHED) {
                throw new OperationFailedException("Событие опубликовано, поэтому отменить его невозможно.");
            }
        }
    }

    private List<UserDto> getInitiators(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().toList();
        List<UserDto> users = userServiceClient.getAllUsers(ids, 0, ids.size());
        if (users.isEmpty()) {
            return users;
        }
        if (users.size() < ids.size()) {
            Set<Long> findUserIds = users.stream().map(UserDto::getId).collect(Collectors.toSet());
            String missingUserIds = ids.stream().filter(id -> !findUserIds.contains(id))
                    .map(Object::toString).collect(Collectors.joining(", "));
            log.debug("Некоторые пользователи не обнаружены при запросе: {}", missingUserIds);
        }
        return users;
    }

    private List<RequestDto> getConfirmedRequests(List<Event> events) {
        log.info("Получаем список подтверждённых запросов для всех событий.");
        List<Long> eventIds = events.stream().map(Event::getId).distinct().toList();
        List<RequestDto> confirmedRequests = new ArrayList<>();
        boolean hasMoreElements = true;
        int from = 0;
        while (hasMoreElements) {
            List<RequestDto> requests = requestClient.getAllRequests(eventIds, null, true, from, 100);
            confirmedRequests.addAll(requests);
            hasMoreElements = requests.size() == 100;
            from += 100;
        }
        return confirmedRequests;
    }

    private List<RecommendedEventProto> getRating(List<Event> events) {
        return statClient.getInteractionsCount(events.stream().map(Event::getId).toList()).toList();
    }
}
