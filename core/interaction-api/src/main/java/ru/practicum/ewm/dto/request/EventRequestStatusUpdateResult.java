package ru.practicum.ewm.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventRequestStatusUpdateResult {
    List<RequestDto> confirmedRequests;
    List<RequestDto> rejectedRequests;
}
