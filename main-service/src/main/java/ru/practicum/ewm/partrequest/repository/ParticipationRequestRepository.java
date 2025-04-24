package ru.practicum.ewm.partrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.partrequest.model.ParticipationRequest;
import ru.practicum.ewm.user.model.User;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    Boolean existsByRequesterAndEvent(User requester, Event event);

    Optional<ParticipationRequest> findByRequesterIdAndId(Long requesterId, Long requestId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventInitiatorIdAndEventId(Long userId, Long eventId);

    @Query("select p from ParticipationRequest p where p.status = 'CONFIRMED' and p.event.id in ?1")
    List<ParticipationRequest> findConfirmedRequests(List<Long> ids);
}
