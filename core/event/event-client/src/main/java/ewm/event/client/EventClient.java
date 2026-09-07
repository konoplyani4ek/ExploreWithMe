package ewm.event.client;

import ewm.event.dto.EventInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "ewm-main-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventInternalDto getEvent(@PathVariable("eventId") long eventId);
}