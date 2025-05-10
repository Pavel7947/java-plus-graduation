package ru.practicum.ewm.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static ru.practicum.ewm.dto.DateTimeFormat.TIME_PATTERN;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CommentDto {
    Long id;
    String text;
    Long eventId;
    String eventName;
    String authorName;
    Integer likes;
    @JsonFormat(pattern = TIME_PATTERN)
    LocalDateTime created;
}
