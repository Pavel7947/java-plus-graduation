package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.api.RequestAdminResource;
import ru.practicum.ewm.dto.RequestSearchFilter;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.service.RequestService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class RequestAdminController implements RequestAdminResource {
    private final RequestService requestService;

    @Override
    public List<RequestDto> getAllRequests(List<Long> eventIds, Boolean confirmed, Integer from, Integer size) {
        log.info("Поступил запрос на получение всех заявок на участие");
        return requestService.getAllRequests(RequestSearchFilter.builder()
                .eventIds(eventIds)
                .confirmed(confirmed)
                .from(from)
                .size(size)
                .build());
    }
}
