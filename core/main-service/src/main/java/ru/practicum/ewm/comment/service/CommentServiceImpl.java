package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.UserServiceClient;
import ru.practicum.ewm.comment.dto.BanCommentDto;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.enums.SortType;
import ru.practicum.ewm.comment.mapper.BanCommentDtoMapper;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.BanComment;
import ru.practicum.ewm.comment.model.BanCommentId;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.BanCommentRepository;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.dto.event.State;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final BanCommentRepository banCommentRepository;
    private final CommentRepository commentRepository;
    private final UserServiceClient userServiceClient;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public CommentDto createComment(Long eventId, Long userId, NewCommentDto newCommentDto) {
        if (checkExistBanForUserAndEvent(userId, eventId)) {
            throw new ValidationException("Для данного пользователя стоит запрет на комментирование данного события");
        }
        checkEventId(eventId);
        Event event = getEventById(eventId);
        if (event.getState() != State.PUBLISHED) {
            throw new ValidationException("Нельзя комментировать не опубликованное событие");
        }
        UserDto userDto = userServiceClient.getUserById(userId);
        if (!event.getCommenting()) {
            throw new ValidationException("Данное событие нельзя комментировать");
        }
        Comment comment = CommentMapper.toComment(newCommentDto, event, userId);
        return CommentMapper.toCommentDto(commentRepository.save(comment), userDto.getName());
    }

    @Transactional
    @Override
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto) {
        checkEventId(eventId);
        UserDto userDto = userServiceClient.getUserById(userId);
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }
        Comment comment = getCommentById(commentId);
        if (!Objects.equals(comment.getEvent().getId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        if (comment.getAuthorId().equals(userId)) {
            comment.setText(newCommentDto.getText());
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }
        return CommentMapper.toCommentDto(comment, userDto.getName());
    }

    @Transactional
    @Override
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        checkEventId(eventId);
        userServiceClient.getUserById(userId);
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }
        Comment comment = getCommentById(commentId);
        if (!Objects.equals(comment.getEvent().getId(), eventId)) {
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
        if (!Objects.equals(comment.getEvent().getId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getAllComments(Long eventId, SortType sortType, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByEvent_Id(eventId, pageable);
        Map<Long, UserDto> users = getAllUsersForComments(comments).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        List<CommentDto> commentDtoList = comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, users.get(comment.getAuthorId()).getName()))
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
        return CommentMapper.toCommentDto(comment, userDto.getName());
    }

    @Transactional
    @Override
    public void deleteLike(Long userId, Long commentId) {
        UserDto userDto = userServiceClient.getUserById(userId);
        Comment comment = getCommentById(commentId);
        if (!comment.getLikes().remove(userId)) {
            throw new NotFoundException("Пользователь не лайкал комментарий с id: " + commentId);
        }
    }

    @Override
    public CommentDto getComment(Long id) {
        Comment comment = getCommentById(id);
        return CommentMapper.toCommentDto(comment, userServiceClient.getUserById(comment.getAuthorId()).getName());
    }


    @Transactional
    @Override
    public BanCommentDto addBanCommited(Long userId, Long eventId) {
        checkEventId(eventId);
        userServiceClient.getUserById(userId);
        getEventById(eventId);
        if (checkExistBanForUserAndEvent(userId, eventId)) {
            throw new ValidationException("Уже добавлен такой запрет на комментирование");
        }
        BanComment newBanComment = BanComment.builder()
                .eventId(eventId)
                .userId(userId)
                .build();
        return BanCommentDtoMapper.mapToBanCommentDto(banCommentRepository.save(newBanComment));
    }

    @Transactional
    @Override
    public BanCommentDto deleteBanCommited(Long userId, Long eventId) {
        checkEventId(eventId);
        BanCommentId banCommentId = BanCommentId.builder()
                .eventId(eventId)
                .userId(userId)
                .build();
        BanComment banComment = banCommentRepository.findById(banCommentId)
                .orElseThrow(() -> new NotFoundException("Такого запрета на комментирование не найдено"));
        banCommentRepository.delete(banComment);
        return BanCommentDtoMapper.mapToBanCommentDto(banComment);
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
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

    private boolean checkExistBanForUserAndEvent(Long userId, Long eventId) {
        return banCommentRepository.existsById(BanCommentId.builder()
                .userId(userId)
                .eventId(eventId)
                .build());
    }

    private List<UserDto> getAllUsersForComments(List<Comment> comments) {
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

}
