package ewm.main.compilation.mapper;

import ewm.main.compilation.model.Compilation;
import ewm.main.dto.CompilationDto;
import ewm.main.dto.NewCompilationDto;

import java.util.List;
import java.util.Set;

public class CompilationMapper {
    public static CompilationDto toDto(Compilation entity, List<Long> eventIds) {
        return CompilationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .pinned(entity.getPinned())
                .events(eventIds)
                .build();
    }

    public static Compilation toEntity(NewCompilationDto dto, Set<Long> eventIds) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .eventIds(eventIds)
                .build();
    }
}