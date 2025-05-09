package ru.practicum.ewm.client.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.ServiceUnavailableException;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {
    private final Throwable throwable;

    @Override
    public UserDto getUserById(Long userId) {
        log.info("При получении пользователя по id возникло исключение: {}", throwable.getClass());
        throw new ServiceUnavailableException(throwable);
    }

    @Override
    public List<UserDto> getAllUsers(List<Long> ids, Integer from, Integer size) {
        log.info("Неудалось получить данные от user-service. Вернул пустой список");
        return List.of();
    }

    @Override
    public UserDto saveUser(NewUserRequest newUserRequest) {
        throw new RuntimeException("Fallback метод не реализован");
    }

    @Override
    public void deleteUser(Long userId) {
        throw new RuntimeException("Fallback метод не реализован");
    }

    @Override
    public UserDto addBanCommited(Long userId, Long eventId) {
        throw new RuntimeException("Fallback метод не реализован");
    }

    @Override
    public void deleteBanCommited(Long userId, Long eventId) {
        throw new RuntimeException("Fallback метод не реализован");
    }
}
