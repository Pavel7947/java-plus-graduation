package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.request.RequestServiceClient;
import ru.practicum.ewm.client.user.UserServiceClient;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.mapper.UserMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.stats.client.StatClient;
import ru.practicum.ewm.stats.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final StatClient statClient;
    private final UserServiceClient userServiceClient;
    private final RequestServiceClient requestClient;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        Set<Long> eventIds = newCompilationDto.getEvents();
        List<Event> events;
        if (eventIds != null && !eventIds.isEmpty()) {
            events = getSeveralEvents(eventIds.stream().toList());
        } else {
            events = Collections.emptyList();
        }
        if (newCompilationDto.getPinned() == null) {
            newCompilationDto.setPinned(false);
        }
        Compilation compilation = compilationRepository.save(CompilationMapper.toCompilation(newCompilationDto, events));
        return CompilationMapper.toCompilationDto(compilation, mapToEventShort(events));

    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id: " + compId + " не найдена"));
        Set<Long> eventIds = updateCompilationRequest.getEvents();
        if (eventIds != null && !eventIds.isEmpty()) {
            compilation.setEvents(new HashSet<>(getSeveralEvents(eventIds.stream().toList())));
        }
        Boolean pinned = updateCompilationRequest.getPinned();
        if (pinned != null) {
            compilation.setPinned(pinned);
        }
        String title = updateCompilationRequest.getTitle();
        if (title != null && !title.isBlank()) {
            compilation.setTitle(title);
        }
        return CompilationMapper.toCompilationDto(compilation, mapToEventShort(new ArrayList<>(compilation.getEvents())));
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id: " + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getAllCompilations(Boolean pinned, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Compilation> allCompilations;
        if (pinned == null) {
            allCompilations = compilationRepository.findAll(pageRequest).toList();
        } else {
            allCompilations = compilationRepository.findAllByPinned(pageRequest, pinned);
        }
        if (allCompilations.isEmpty()) {
            return List.of();
        }
        Map<Long, EventShortDto> allEventDto = mapToEventShort(allCompilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream()).distinct().toList())
                .stream().collect(Collectors.toMap(EventShortDto::getId, Function.identity()));
        List<CompilationDto> compilationDtoList = new ArrayList<>();
        for (Compilation compilation : allCompilations) {
            List<EventShortDto> listEventDto = compilation.getEvents().stream().map(event -> allEventDto.get(event.getId()))
                    .toList();
            compilationDtoList.add(CompilationMapper.toCompilationDto(compilation, listEventDto));
        }
        return compilationDtoList;

    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с id: " + compId + " не найдена"));
        return CompilationMapper.toCompilationDto(compilation, mapToEventShort(new ArrayList<>(compilation.getEvents())));
    }

    private List<Event> getSeveralEvents(List<Long> eventIds) {
        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Не удалось найти некоторые события в базе данных");
        }
        return events;
    }

    private List<EventShortDto> mapToEventShort(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDateTime minTime = events.stream().map(Event::getCreatedOn).min(Comparator.comparing(Function.identity())).get();
        List<String> urisList = events.stream().map(event -> "/events/" + event.getId()).toList();
        String uris = String.join(", ", urisList);
        List<StatsDto> statsList = statClient.getStats(minTime.minusSeconds(1), LocalDateTime.now(), uris, false);
        Map<Long, UserDto> initiators = getAllInitiators(events).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        Map<Long, Integer> confirmedRequestsCount = getConfirmedRequestsForEvents(events).stream()
                .collect(Collectors.groupingBy(RequestDto::getEvent, Collectors.reducing(0, e -> 1, Integer::sum)));
        return events.stream().map(event -> {
                    Optional<StatsDto> result = statsList.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + event.getId()))
                            .findFirst();
                    UserDto initiator = initiators.get(event.getInitiatorId());
                    var requestsCount = confirmedRequestsCount.get(event.getId());
                    if (result.isPresent()) {
                        return EventMapper.mapToShortDto(event, result.get().getHits(),
                                initiator != null ? UserMapper.mapToUserShort(initiator) : null,
                                requestsCount != null ? requestsCount : 0);
                    } else {
                        return EventMapper.mapToShortDto(event, 0L,
                                initiator != null ? UserMapper.mapToUserShort(initiator) : null,
                                requestsCount != null ? requestsCount : 0);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<UserDto> getAllInitiators(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().toList();
        List<UserDto> users = userServiceClient.getAllUsers(ids, 0, ids.size());
        if (users.size() < ids.size()) {
            Set<Long> findUserIds = users.stream().map(UserDto::getId).collect(Collectors.toSet());
            String missingUserIds = ids.stream().filter(id -> !findUserIds.contains(id))
                    .map(Object::toString).collect(Collectors.joining(", "));
            log.debug("Некоторые пользователи не обнаружены при запросе: {}", missingUserIds);
            throw new RuntimeException();
        }
        return users;
    }

    private List<RequestDto> getConfirmedRequestsForEvents(List<Event> events) {
        log.info("Получаем список подтверждённых запросов для всех событий.");
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<RequestDto> confirmedRequests = new ArrayList<>();
        boolean hasMoreElements = true;
        int from = 0;
        while (hasMoreElements) {
            List<RequestDto> requests = requestClient.getAllRequests(eventIds, true, from, 100);
            confirmedRequests.addAll(requests);
            hasMoreElements = requests.size() == 100;
            from += 100;
        }
        return confirmedRequests;
    }
}

