package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users")
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    Long id;
    @Column(unique = true)
    String email;
    String name;
    @ElementCollection
    @CollectionTable(name = "ban_comments", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "event_id")
    @Builder.Default
    Set<Long> forbiddenCommentEvents = new HashSet<>();
}
