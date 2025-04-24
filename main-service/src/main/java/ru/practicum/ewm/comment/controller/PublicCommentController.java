package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.enums.SortType;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
@Slf4j
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/{eventId}")
    private List<CommentDto> getAllCommentsByEventId(@PathVariable Long eventId,
                                                     @RequestParam(defaultValue = "LIKES") SortType sort,
                                                     @RequestParam(defaultValue = "0") Integer from,
                                                     @RequestParam(defaultValue = "20") Integer size) {
        return commentService.getAllComments(eventId, sort, from, size);
    }
}
