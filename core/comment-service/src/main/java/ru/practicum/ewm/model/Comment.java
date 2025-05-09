package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    Long id;

    String text;

    @Column(name = "event_id")
    Long eventId;

    @Column(name = "author_id")
    Long authorId;

    @ElementCollection
    @CollectionTable(name = "comments_likes", joinColumns = @JoinColumn(name = "comment_id"))
    @Column(name = "user_id")
    final Set<Long> likes = new HashSet<>();

    LocalDateTime created;
}
