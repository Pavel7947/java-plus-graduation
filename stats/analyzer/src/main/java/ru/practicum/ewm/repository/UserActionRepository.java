package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.dto.WeightSum;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.model.UserActionId;

import java.util.List;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, UserActionId> {

    List<UserAction> findAllByUserIdAndEventIdIn(Long userId, Set<Long> eventIds);

    List<UserAction> findAllByUserId(Long userId, Pageable pageable);

    @Query("select new ru.practicum.ewm.dto.WeightSum(ua.eventId, sum(ua.weight)) " +
            "from UserAction ua where ua.eventId in (:eventIds) group by ua.eventId")
    List<WeightSum> getWeightSumByEventIds(Set<Long> eventIds);
}
