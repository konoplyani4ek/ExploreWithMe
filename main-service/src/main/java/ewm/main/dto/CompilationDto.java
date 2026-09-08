package ewm.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    // Временно — id событий вместо полных EventShortDto. Событие теперь в event-service,
    // а батч-получение краткого представления по списку id — задача этапа
    // "дополнительная функциональность".
    private List<Long> events;

    @NotNull
    private Long id;

    @NotNull
    private Boolean pinned;

    @NotBlank
    @Size(max = 50, message = "The name must not exceed 50 characters")
    private String title;
}