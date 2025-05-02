package ru.practicum.ewm.partrequest.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.partrequest.dto.ParticipationRequestDto;
import ru.practicum.ewm.partrequest.model.ParticipationRequest;

import java.util.List;

@UtilityClass
public class ParticipationRequestMapper {

    public ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .requester(request.getRequester().getId())
                .event(request.getEvent().getId())
                .created(request.getCreated())
                .status(request.getStatus())
                .build();
    }

    public List<ParticipationRequestDto> toParticipationRequestDto(List<ParticipationRequest> requests) {
        return requests.stream().map(ParticipationRequestMapper::toParticipationRequestDto).toList();
    }
}
