package ewm.place.client;

import ewm.place.dto.PlaceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * additional-service — полноценный отдельный сервис (был временно частью main-service).
 */
@FeignClient(name = "additional-service", path = "/internal/places")
public interface PlaceClient {

    @GetMapping("/{placeId}")
    PlaceDto getPlace(@PathVariable("placeId") long placeId);
}