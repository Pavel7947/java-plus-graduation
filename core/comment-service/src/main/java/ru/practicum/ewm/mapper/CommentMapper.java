package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.model.Comment;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {

    public CommentDto toCommentDto(Comment comment, UserDto author, EventFullDto eventFullDto) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(eventFullDto.getId())
                .eventName(eventFullDto.getAnnotation())
                .authorName(author.getName())
                .likes(comment.getLikes().size())
                .created(comment.getCreated())
                .build();
    }

    public Comment toComment(NewCommentDto newCommentDto, Long eventId, Long userId) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .eventId(eventId)
                .authorId(userId)
                .created(LocalDateTime.now())
                .build();
    }
}
