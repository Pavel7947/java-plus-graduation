package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "events_similarity")
@IdClass(EventSimilarityId.class)
public class EventSimilarity {
    @Id
    @Column(name = "event_a")
    private Long eventA;
    @Id
    @Column(name = "event_b")
    private Long eventB;
    @Column(nullable = false)
    private Double score;
    @Column(nullable = false)
    private Instant actionDate;
}
