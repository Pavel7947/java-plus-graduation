package ru.practicum.ewm.partrequest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.partrequest.dto.ParticipationRequestDto;
import ru.practicum.ewm.partrequest.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@Slf4j
@RequiredArgsConstructor
public class ParticipationRequestController {
    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getAllUserRequests(@PathVariable Long userId) {
        log.info("Поступил запрос от пользователя с id: {} на получение всех его ParticipationRequest", userId);
        return requestService.getAllUserRequests(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") Long eventId) {
        log.info("Поступил запрос от пользователя с id: {} на добавление ParticipationRequest для события с id: {}",
                userId, eventId);
        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        log.info("Поступил запрос от пользователя с id: {} на отклонение ParticipationRequest с id: {}",
                userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}
