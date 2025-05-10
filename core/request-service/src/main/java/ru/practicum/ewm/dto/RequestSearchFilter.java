package ru.practicum.ewm.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class RequestSearchFilter {
    List<Long> eventIds;
    Boolean confirmed;
    Integer from;
    Integer size;
}
