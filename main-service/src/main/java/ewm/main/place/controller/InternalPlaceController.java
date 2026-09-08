package ewm.main.place.controller;

import ewm.main.exception.NotFoundException;
import ewm.main.place.Place;
import ewm.main.place.repository.PlaceRepository;
import ewm.place.dto.PlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/places")
@RequiredArgsConstructor
public class InternalPlaceController {

    private final PlaceRepository placeRepository;

    @GetMapping("/{placeId}")
    public PlaceDto getPlace(@PathVariable long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Место с id=" + placeId + " не найдено"));

        return PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .lat(place.getLat())
                .lon(place.getLon())
                .build();
    }
}