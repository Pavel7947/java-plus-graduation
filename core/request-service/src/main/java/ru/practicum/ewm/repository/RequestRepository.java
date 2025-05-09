package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.ewm.dto.request.Status;
import ru.practicum.ewm.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long>,
        QuerydslPredicateExecutor<Request> {

    Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    Optional<Request> findByRequesterIdAndId(Long requesterId, Long requestId);

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByStatusAndEventId(Status status, Long eventId);
}
