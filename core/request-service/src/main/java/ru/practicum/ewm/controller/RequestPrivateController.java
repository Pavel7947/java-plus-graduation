package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@Slf4j
@RequiredArgsConstructor
@Validated
public class RequestPrivateController {
    private final RequestService requestService;

    @GetMapping("/requests")
    public List<RequestDto> getAllUserRequests(@PathVariable Long userId) {
        log.info("Поступил запрос от пользователя с id: {} на получение всех его Request", userId);
        return requestService.getAllUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto addRequest(@PathVariable Long userId,
                                 @RequestParam(defaultValue = "0") Long eventId) {
        log.info("Поступил запрос от пользователя с id: {} на добавление Request для события с id: {}",
                userId, eventId);
        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public RequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        log.info("Поступил запрос от пользователя с id: {} на отклонение Request с id: {}",
                userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("events/{eventId}/requests")
    public List<RequestDto> getRequestsOfUserEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("Поступил запрос на получение всех заявок на событие с id: {} от пользователя с id: {}", eventId, userId);
        return requestService.getRequestsOfUserEvent(userId, eventId);
    }

    @PatchMapping("events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestsStatus(@PathVariable Long userId, @PathVariable Long eventId,
                                                               @Valid @RequestBody
                                                               EventRequestStatusUpdateRequest updateRequest) {
        log.info("Получили запрос на обновление статусов заявок");
        return requestService.updateRequestsStatus(userId, eventId, updateRequest);
    }
}