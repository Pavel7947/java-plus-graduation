package ru.practicum.ewm.comment.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.comment.dto.BanCommentDto;
import ru.practicum.ewm.comment.model.BanComment;

@UtilityClass
public class BanCommentDtoMapper {

    public BanCommentDto mapToBanCommentDto(BanComment banComment) {
        return BanCommentDto.builder()
                .eventId(banComment.getEventId())
                .userId(banComment.getUserId())
                .build();
    }
}
