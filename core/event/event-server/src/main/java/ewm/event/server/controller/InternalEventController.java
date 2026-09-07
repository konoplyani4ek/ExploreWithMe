package ewm.event.server.controller;

import ewm.event.dto.EventInternalDto;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.model.Event;
import ewm.event.server.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Внутренний (межсервисный) контроллер — НЕ проксируется через Gateway.
 * Потребитель — request-service (EventClient), валидирует заявку на участие.
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(@PathVariable long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .build();
    }
}