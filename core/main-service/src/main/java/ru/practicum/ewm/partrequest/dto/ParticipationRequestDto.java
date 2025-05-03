package ru.practicum.ewm.partrequest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.partrequest.enums.Status;

import java.time.LocalDateTime;

import static ru.practicum.ewm.dto.DateTimeFormat.TIME_PATTERN;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    Long id;
    @JsonFormat(pattern = TIME_PATTERN)
    LocalDateTime created;
    Long event;
    Long requester;
    Status status;
}
