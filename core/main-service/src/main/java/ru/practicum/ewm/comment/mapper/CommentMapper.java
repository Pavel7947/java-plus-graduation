package ru.practicum.ewm.comment.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.event.model.Event;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {

    public CommentDto toCommentDto(Comment comment, String authorName) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .eventName(comment.getEvent().getAnnotation())
                .authorName(authorName)
                .likes(comment.getLikes().size())
                .created(comment.getCreated())
                .build();
    }

    public Comment toComment(NewCommentDto newCommentDto, Event event, Long userId) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .event(event)
                .authorId(userId)
                .created(LocalDateTime.now())
                .build();
    }
}
