package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.model.Request;

import java.util.List;

@UtilityClass
public class RequestMapper {

    public RequestDto toRequestDto(Request request) {
        return RequestDto.builder()
                .id(request.getId())
                .requester(request.getRequesterId())
                .event(request.getEventId())
                .created(request.getCreated())
                .status(request.getStatus())
                .build();
    }

    public List<RequestDto> toRequestDto(List<Request> requests) {
        return requests.stream().map(RequestMapper::toRequestDto).toList();
    }
}
