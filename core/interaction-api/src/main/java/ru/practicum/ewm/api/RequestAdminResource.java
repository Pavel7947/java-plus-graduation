package ru.practicum.ewm.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.request.RequestDto;

import java.util.List;

public interface RequestAdminResource {

    @GetMapping("admin/requests")
    List<RequestDto> getAllRequests(@RequestParam(required = false) List<Long> eventIds,
                                    @RequestParam(required = false) List<Long> userIds,
                                    @RequestParam(required = false) Boolean confirmed,
                                    @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                    @Positive @RequestParam(defaultValue = "10") Integer size);

}
