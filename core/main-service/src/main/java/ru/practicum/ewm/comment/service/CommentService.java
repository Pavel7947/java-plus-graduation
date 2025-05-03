package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.BanCommentDto;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.enums.SortType;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long eventId, Long userId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto);

    void deleteComment(Long userId, Long eventId, Long commentId);

    void deleteComment(Long commentId, Long eventId);

    List<CommentDto> getAllComments(Long eventId, SortType sortType, Integer from, Integer size);

    CommentDto addLike(Long userId, Long commentId);

    BanCommentDto addBanCommited(Long userId, Long eventId);

    BanCommentDto deleteBanCommited(Long userId, Long eventId);

    void deleteLike(Long userId, Long commentId);

    CommentDto getComment(Long id);
}
