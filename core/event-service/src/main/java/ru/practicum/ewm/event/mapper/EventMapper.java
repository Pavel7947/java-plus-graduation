package ru.practicum.ewm.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.stats.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class EventMapper {
    public Event mapToEvent(NewEventDto eventDto, Category category, Long initiatorId) {
        return Event.builder()
                .eventDate(eventDto.getEventDate())
                .annotation(eventDto.getAnnotation())
                .paid(eventDto.getPaid())
                .category(category)
                .createdOn(LocalDateTime.now())
                .description(eventDto.getDescription())
                .state(State.PENDING)
                .title(eventDto.getTitle())
                .lat(eventDto.getLocation().getLat())
                .lon(eventDto.getLocation().getLon())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .initiatorId(initiatorId)
                .commenting(eventDto.getCommenting())
                .build();
    }

    public EventFullDto mapToFullDto(Event event, Long views, UserShortDto initiator, Integer confirmedRequests) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .location(Location.builder().lat(event.getLat()).lon(event.getLon()).build())
                .paid(event.getPaid())
                .views(views)
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .commenting(event.getCommenting())
                .build();
    }

    public EventShortDto mapToShortDto(Event event, Long views, UserShortDto initiator, Integer confirmedRequests) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .publishedOn(event.getPublishedOn())
                .id(event.getId())
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .commenting(event.getCommenting())
                .build();
    }

    public List<EventShortDto> mapToShortDto(List<Event> events, List<UserDto> initiators, List<StatsDto> statsList,
                                             List<RequestDto> confirmedRequests) {
        Map<Long, Integer> confirmedRequestsCountMap = confirmedRequests.stream()
                .collect(Collectors.groupingBy(RequestDto::getEvent, Collectors.reducing(0, e -> 1, Integer::sum)));
        return mapToShortDto(events, initiators, statsList, confirmedRequestsCountMap);
    }

    public List<EventShortDto> mapToShortDto(List<Event> events, List<UserDto> initiators, List<StatsDto> statsList,
                                             Map<Long, Integer> confirmedRequestsCountMap) {
        Map<Long, UserDto> initiatorsMap = initiators.stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        return events.stream().map(event -> {
            Long eventId = event.getId();
            Optional<StatsDto> stat = statsList.stream()
                    .filter(statsDto -> statsDto.getUri().equals("/events/" + eventId))
                    .findFirst();
            Integer confirmedRequestsCount = confirmedRequestsCountMap.get(eventId);
            UserDto initiator = initiatorsMap.get(event.getInitiatorId());
            return mapToShortDto(event,
                    stat.isPresent() ? stat.get().getHits() : 0L,
                    initiator != null ? UserMapper.mapToUserShort(initiator) : null,
                    confirmedRequestsCount != null ? confirmedRequestsCount : 0);
        }).toList();
    }

    public List<EventFullDto> mapToFullDto(List<Event> events, List<UserDto> initiators, List<StatsDto> statsList,
                                           List<RequestDto> confirmedRequests) {
        Map<Long, Integer> confirmedRequestsCountMap = confirmedRequests.stream()
                .collect(Collectors.groupingBy(RequestDto::getEvent, Collectors.reducing(0, e -> 1, Integer::sum)));
        Map<Long, UserDto> initiatorsMap = initiators.stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        return events.stream().map(event -> {
                    Long eventId = event.getId();
                    Optional<StatsDto> stat = statsList.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + eventId))
                            .findFirst();
                    UserDto initiator = initiatorsMap.get(event.getInitiatorId());
                    var requests = confirmedRequestsCountMap.get(eventId);
                    return mapToFullDto(event, stat.isPresent() ? stat.get().getHits() : 0L,
                            initiator != null ? UserMapper.mapToUserShort(initiator) : null,
                            requests != null ? requests : 0);
                })
                .toList();
    }
}
