package ru.practicum.ewm.compilation.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;

import java.util.HashSet;
import java.util.List;

@UtilityClass
public class CompilationMapper {

    public Compilation toCompilation(NewCompilationDto compilationDto, List<Event> events) {
        return Compilation.builder()
                .pinned(compilationDto.getPinned())
                .title(compilationDto.getTitle())
                .events(new HashSet<>(events))
                .build();
    }

    public CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> listEventDto) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(listEventDto)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();


    }
}
