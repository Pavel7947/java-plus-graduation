package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.dto.event.Location;

import java.time.LocalDateTime;

import static ru.practicum.ewm.dto.DateTimeFormat.TIME_PATTERN;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewEventDto {
    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;
    @NotNull
    Long category;
    @NotBlank
    @Size(min = 20, max = 7000)
    String description;
    @JsonFormat(pattern = TIME_PATTERN)
    @Future
    @NotNull
    LocalDateTime eventDate;
    @NotNull
    Location location;
    Boolean paid;
    @PositiveOrZero
    Integer participantLimit;
    Boolean requestModeration;
    @NotBlank
    @Size(min = 3, max = 120)
    String title;
    Boolean commenting;
}
