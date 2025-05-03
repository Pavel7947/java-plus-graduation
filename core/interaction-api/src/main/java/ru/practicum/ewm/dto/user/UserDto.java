package ru.practicum.ewm.dto.user;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    Long id;
    String email;
    String name;
}
