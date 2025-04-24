package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewUserRequest {
    @Email
    @NotBlank
    @Size(min = 6, max = 254)
    String email;
    @NotBlank
    @Size(min = 2, max = 250)
    String name;
}
