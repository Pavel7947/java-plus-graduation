package ru.practicum.ewm.comment.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;


@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode
public class BanCommentId implements Serializable {
    Long eventId;
    Long userId;
}
