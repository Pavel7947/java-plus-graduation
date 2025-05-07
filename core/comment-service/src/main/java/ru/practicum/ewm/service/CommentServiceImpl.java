package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.EventServiceClient;
import ru.practicum.ewm.client.UserServiceClient;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.enums.SortType;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.repository.CommentRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventClient;

    @Transactional
    @Override
    public CommentDto createComment(Long eventId, Long userId, NewCommentDto newCommentDto) {
        UserDto userDto = userServiceClient.getUserById(userId);
        if (userDto.getForbiddenCommentEvents().contains(eventId)) {
            throw new ValidationException("Для данного пользователя стоит запрет на комментирование данного события");
        }
        checkEventId(eventId);
        EventFullDto event = eventClient.getEventById(eventId, false, false);
        if (event.getState() != State.PUBLISHED) {
            throw new ValidationException("Нельзя комментировать не опубликованное событие");
        }
        if (!event.getCommenting()) {
            throw new ValidationException("Данное событие нельзя комментировать");
        }
        Comment comment = CommentMapper.toComment(newCommentDto, eventId, userId);
        return CommentMapper.toCommentDto(commentRepository.save(comment), userDto, event);
    }

    @Transactional
    @Override
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto) {
        checkEventId(eventId);
        UserDto userDto = userServiceClient.getUserById(userId);
        EventFullDto event = eventClient.getEventById(eventId, false, false);
        Comment comment = getCommentById(commentId);
        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        if (comment.getAuthorId().equals(userId)) {
            comment.setText(newCommentDto.getText());
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }
        return CommentMapper.toCommentDto(comment, userDto, event);
    }

    @Transactional
    @Override
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        checkEventId(eventId);
        userServiceClient.getUserById(userId);
        eventClient.getEventById(eventId, false, false);
        Comment comment = getCommentById(commentId);
        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        if (comment.getAuthorId().equals(userId)) {
            commentRepository.deleteById(commentId);
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }
    }

    @Transactional
    @Override
    public void deleteComment(Long commentId, Long eventId) {
        checkEventId(eventId);
        Comment comment = getCommentById(commentId);
        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getAllComments(Long eventId, SortType sortType, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        Map<Long, UserDto> users = getAuthors(comments).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        Map<Long, EventFullDto> events = getEventsForComments(comments).stream()
                .collect(Collectors.toMap(EventFullDto::getId, Function.identity()));
        List<CommentDto> commentDtoList = comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, users.get(comment.getAuthorId()),
                        events.get(comment.getEventId())))
                .toList();
        if (sortType == SortType.LIKES) {
            return commentDtoList.stream().sorted(Comparator.comparing(CommentDto::getLikes).reversed()).toList();
        } else {
            return commentDtoList.stream().sorted(Comparator.comparing(CommentDto::getCreated).reversed()).toList();
        }
    }

    @Transactional
    @Override
    public CommentDto addLike(Long userId, Long commentId) {
        UserDto userDto = userServiceClient.getUserById(userId);
        Comment comment = getCommentById(commentId);
        if (comment.getAuthorId().equals(userId)) {
            throw new ValidationException("Пользователь не может лайкать свой комментарий");
        }
        if (!comment.getLikes().add(userId)) {
            throw new ValidationException("Нельзя поставить лайк второй раз");
        }
        EventFullDto eventFullDto = eventClient.getEventById(comment.getEventId(), false, false);
        return CommentMapper.toCommentDto(comment, userDto, eventFullDto);
    }

    @Transactional
    @Override
    public void deleteLike(Long userId, Long commentId) {
        userServiceClient.getUserById(userId);
        Comment comment = getCommentById(commentId);
        if (!comment.getLikes().remove(userId)) {
            throw new NotFoundException("Пользователь не лайкал комментарий с id: " + commentId);
        }
    }

    @Override
    public CommentDto getComment(Long id) {
        Comment comment = getCommentById(id);
        return CommentMapper.toCommentDto(comment, userServiceClient.getUserById(comment.getAuthorId()),
                eventClient.getEventById(comment.getEventId(), false, false));
    }


    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
    }

    private void checkEventId(Long eventId) {
        if (eventId == 0) {
            throw new ValidationException("Не задан eventId");
        }
    }

    private List<UserDto> getAuthors(List<Comment> comments) {
        List<Long> ids = comments.stream().map(Comment::getAuthorId).distinct().toList();
        List<UserDto> users = userServiceClient.getAllUsers(ids, 0, ids.size());
        if (users.size() < ids.size()) {
            Set<Long> findUserIds = users.stream().map(UserDto::getId).collect(Collectors.toSet());
            String missingUserIds = ids.stream().filter(id -> !findUserIds.contains(id))
                    .map(Object::toString).collect(Collectors.joining(", "));
            log.debug("Некоторые пользователи не обнаружены при запросе: {}", missingUserIds);
            throw new RuntimeException();
        }
        return users;
    }

    private List<EventFullDto> getEventsForComments(List<Comment> comments) {
        List<Long> ids = comments.stream().map(Comment::getEventId).distinct().toList();
        List<EventFullDto> events = eventClient.getEventsForAdmin(ids,
                null, null, null, false, null, null,
                0, ids.size());
        if (events.size() < ids.size()) {
            Set<Long> findEventsIds = events.stream().map(EventFullDto::getId).collect(Collectors.toSet());
            String missingEventIds = ids.stream().filter(id -> !findEventsIds.contains(id))
                    .map(Object::toString).collect(Collectors.joining(", "));
            log.debug("Некоторые события не обнаружены при запросе: {}", missingEventIds);
            throw new RuntimeException();
        }
        return events;
    }

}
