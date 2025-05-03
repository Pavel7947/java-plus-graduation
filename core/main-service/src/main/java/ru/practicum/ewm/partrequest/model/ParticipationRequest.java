package ru.practicum.ewm.partrequest.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.partrequest.enums.Status;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    Long id;
    @Builder.Default
    LocalDateTime created = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "event_id")
    Event event;
    @Column(name = "requester_id")
    Long requesterId;
    @Enumerated(value = EnumType.STRING)
    Status status;


}
