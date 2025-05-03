package ru.practicum.ewm.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

public interface UserServiceAdminResource {

    @GetMapping("/admin/users/{userId}")
    UserDto getUserById(@PathVariable Long userId);

    @GetMapping("/admin/users")
    List<UserDto> getAllUsers(@RequestParam(defaultValue = "") List<Long> ids,
                              @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                              @Positive @RequestParam(defaultValue = "10") Integer size);

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    UserDto saveUser(@RequestBody NewUserRequest newUserRequest);

    @DeleteMapping("/admin/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@PathVariable Long userId);
}
