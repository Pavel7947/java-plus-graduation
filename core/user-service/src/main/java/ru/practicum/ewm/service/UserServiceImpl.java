package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.EventServiceClient;
import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.DuplicateException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.UserDtoMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EventServiceClient eventClient;

    @Override
    public List<UserDto> getAllUsers(List<Long> ids, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);

        if (ids.isEmpty()) {
            return userRepository.findAll(pageRequest).getContent().stream()
                    .map(UserDtoMapper::toUserDto)
                    .toList();
        } else {
            return userRepository.findAllByIdIn(ids, pageRequest).getContent().stream()
                    .map(UserDtoMapper::toUserDto)
                    .toList();
        }
    }

    @Transactional
    @Override
    public UserDto saveUser(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new DuplicateException("Пользователь с таким email уже существует");
        }
        return UserDtoMapper.toUserDto(userRepository.save(UserDtoMapper.toUser(newUserRequest)));
    }

    @Transactional
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserDto getUserById(Long userId) {
        return UserDtoMapper.toUserDto(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + ". Не найден")));
    }

    @Transactional
    @Override
    public UserDto addBanCommited(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + ". Не найден"));
        eventClient.getEventById(eventId, false, false);
        Set<Long> forbiddenCommentEvents = user.getForbiddenCommentEvents();
        if (forbiddenCommentEvents.contains(eventId)) {
            throw new ValidationException("Уже добавлен такой запрет на комментирование");
        }
        forbiddenCommentEvents.add(eventId);
        return UserDtoMapper.toUserDto(user);
    }

    @Transactional
    @Override
    public void deleteBanCommited(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + ". Не найден"));
        if (!user.getForbiddenCommentEvents().remove(eventId)) {
            throw new NotFoundException("Такого запрета на комментирование не найдено");
        }
    }
}
