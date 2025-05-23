package ru.practicum.ewm.controller;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.api.UserServiceAdminResource;
import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserAdminController implements UserServiceAdminResource {
    private final UserService userService;

    @Override
    public List<UserDto> getAllUsers(List<Long> ids, @PositiveOrZero Integer from,
                                     @Positive Integer size) {
        log.info("Пришел запрос на получение списка пользователей");
        return userService.getAllUsers(ids, from, size);
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.info("Пришел запрос на получение пользователя по id");
        return userService.getUserById(userId);
    }

    @Override
    public UserDto saveUser(@Validated NewUserRequest newUserRequest) {
        log.info("Пришел запрос на добавление пользователя");
        return userService.saveUser(newUserRequest);
    }

    @Override
    public void deleteUser(Long userId) {
        log.info("Пришел запрос на удаление пользователя");
        userService.deleteUser(userId);
    }

    @Override
    public UserDto addBanCommited(Long userId, Long eventId) {
        log.info("Получили запрос от администартора на добавление запрета комментирования");
        return userService.addBanCommited(userId, eventId);
    }

    @Override
    public void deleteBanCommited(Long userId, Long eventId) {
        log.info("Получили запрос от администратора на отмену запрета комментирования");
        userService.deleteBanCommited(userId, eventId);
    }
}
