package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.time.LocalDateTime;

import static ru.practicum.ewm.dto.DateTimeFormat.TIME_PATTERN;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventShortDto {
    Long id;
    String annotation;
    CategoryDto category;
    Integer confirmedRequests;
    @JsonFormat(pattern = TIME_PATTERN)
    LocalDateTime eventDate;
    LocalDateTime publishedOn;
    UserShortDto initiator;
    Boolean paid;
    String title;
    Long views;
    Boolean commenting;
}
