package ru.practicum.ewm.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.model.EventSimilarityId;

import java.util.List;
import java.util.Set;

public interface EventsSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    List<EventSimilarity> findAllByEventAOrEventB(Long eventA, Long eventB, Sort sort);

    List<EventSimilarity> findAllByEventAInOrEventBIn(Set<Long> eventIds, Set<Long> eventBIds, Sort sort);

}
