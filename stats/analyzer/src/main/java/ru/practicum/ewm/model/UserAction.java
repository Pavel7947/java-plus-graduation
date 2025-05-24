package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@Table(name = "users_actions")
@IdClass(UserActionId.class)
@AllArgsConstructor
@NoArgsConstructor
public class UserAction {
    @Id
    private Long eventId;
    @Id
    private Long userId;
    @Column(nullable = false)
    private Instant lastActionDate;
    @Column(nullable = false)
    private Double weight;
}
