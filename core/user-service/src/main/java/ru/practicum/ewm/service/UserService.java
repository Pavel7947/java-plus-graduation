package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> getAllUsers(List<Long> ids, Integer from, Integer size);

    UserDto saveUser(NewUserRequest newUserRequest);

    void deleteUser(Long id);

    UserDto getUserById(Long userId);
}
