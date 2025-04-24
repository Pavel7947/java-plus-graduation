package ru.practicum.ewm.user.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserShortDto {
    Long id;
    String name;
}
