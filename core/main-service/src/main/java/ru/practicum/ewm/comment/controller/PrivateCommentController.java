package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
@Slf4j
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId, @RequestParam(defaultValue = "0") Long eventId,
                                    @RequestBody NewCommentDto newCommentDto) {
        log.info("Получили запрос на создание комментария {}", newCommentDto);
        return commentService.createComment(eventId, userId, newCommentDto);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId, @PathVariable Long commentId,
                                    @RequestParam(defaultValue = "0") Long eventId,
                                    @RequestBody NewCommentDto newCommentDto) {
        log.info("Получили запрос на редактирование комментария {}", newCommentDto);
        return commentService.updateComment(userId, eventId, commentId, newCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId, @PathVariable Long commentId,
                              @RequestParam(defaultValue = "0") Long eventId) {
        log.info("Получили запрос на удаление комментария");
        commentService.deleteComment(userId, eventId, commentId);
    }

    @PutMapping("/{commentId}/like")
    public CommentDto addLike(@PathVariable Long userId, @PathVariable Long commentId) {
        log.info("Получили запрос на добавление лайка");
        return commentService.addLike(userId, commentId);
    }

    @DeleteMapping("/{commentId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLike(@PathVariable Long userId, @PathVariable Long commentId) {
        log.info("Получили запрос на удаление лайка");
        commentService.deleteLike(userId, commentId);
    }
}
