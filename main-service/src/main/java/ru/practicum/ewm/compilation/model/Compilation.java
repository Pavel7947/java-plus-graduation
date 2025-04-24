package ru.practicum.ewm.compilation.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.event.model.Event;

import java.util.Set;

@Entity
@Table(name = "compilations")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "compilation_id")
    Long id;
    Boolean pinned;
    String title;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    Set<Event> events;
}
