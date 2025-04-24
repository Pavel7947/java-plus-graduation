package ru.practicum.ewm.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.event.enums.UpdateStatus;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventRequestStatusUpdateRequest {
    final List<Long> requestIds = new ArrayList<>();
    @NotNull
    UpdateStatus status;
}
