package ru.practicum.ewm.stats.server.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.StatsDto;
import ru.practicum.ewm.stats.server.mapper.DtoMapper;
import ru.practicum.ewm.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new ValidationException("End не может быть раньше start");
        }

        if (!uris.isEmpty() && unique) {
            return statsRepository.getStatsByUriWithUniqueIp(start, end, uris);
        } else if (uris.isEmpty() && unique) {
            return statsRepository.getStatsWithUniqueIp(start, end);
        } else if (!uris.isEmpty()) {
            return statsRepository.getStatsByUri(start, end, uris);
        } else {
            return statsRepository.getStats(start, end);
        }
    }

    @Transactional
    @Override
    public void saveHit(EndpointHitDto endpointHitDto) {
        statsRepository.save(DtoMapper.toEndpointHit(endpointHitDto));
    }
}
