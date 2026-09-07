package ewm.event.server.service;

import ewm.category.client.CategoryClient;
import ewm.category.dto.CategoryDto;
import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.mapper.EventMapper;
import ewm.event.server.model.Event;
import ewm.event.server.stat.StatService;
import ewm.place.client.PlaceClient;
import ewm.place.dto.PlaceDto;
import ewm.request.client.RequestClient;
import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.stat.client.model.GetStatsParams;
import ewm.user.client.UserClient;
import ewm.user.dto.UserDto;
import ewm.user.dto.UserShortDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EventDtoAssembler {
    private static final boolean UNIQUE_VIEWS = true;

    private final StatService statService;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final PlaceClient placeClient;

    public EventDtoAssembler(StatService statService,
                             RequestClient requestClient,
                             UserClient userClient,
                             CategoryClient categoryClient,
                             PlaceClient placeClient) {
        this.statService = statService;
        this.requestClient = requestClient;
        this.userClient = userClient;
        this.categoryClient = categoryClient;
        this.placeClient = placeClient;
    }

    public EventShortDto toShortDto(Event event) {
        UserShortDto initiator = getInitiator(event.getInitiatorId());
        CategoryDto category = categoryClient.getCategory(event.getCategoryId());

        EventShortDto dto = EventMapper.toShortDto(event, category, initiator);

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(requestClient.getConfirmedCount(event.getId()));

        return dto;
    }

    public EventFullDto toFullDto(Event event) {
        UserShortDto initiator = getInitiator(event.getInitiatorId());
        CategoryDto category = categoryClient.getCategory(event.getCategoryId());
        PlaceDto place = event.getPlaceId() != null ? placeClient.getPlace(event.getPlaceId()) : null;

        EventFullDto dto = EventMapper.toFullDto(event, category, initiator, place);

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(requestClient.getConfirmedCount(event.getId()));

        return dto;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);
        Map<Long, UserShortDto> initiatorsByUserId = getInitiatorsByUserId(events);
        Map<Long, CategoryDto> categoriesByCategoryId = getCategoriesByCategoryId(events);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = EventMapper.toShortDto(
                    event,
                    categoriesByCategoryId.get(event.getCategoryId()),
                    initiatorsByUserId.get(event.getInitiatorId())
            );
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(confirmedRequestsByEventId.getOrDefault(event.getId(), 0L));
            result.add(dto);
        }

        return result;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);
        Map<Long, UserShortDto> initiatorsByUserId = getInitiatorsByUserId(events);
        Map<Long, CategoryDto> categoriesByCategoryId = getCategoriesByCategoryId(events);

        List<EventFullDto> result = new ArrayList<>();

        for (Event event : events) {
            PlaceDto place = event.getPlaceId() != null ? placeClient.getPlace(event.getPlaceId()) : null;

            EventFullDto dto = EventMapper.toFullDto(
                    event,
                    categoriesByCategoryId.get(event.getCategoryId()),
                    initiatorsByUserId.get(event.getInitiatorId()),
                    place
            );
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(confirmedRequestsByEventId.getOrDefault(event.getId(), 0L));
            result.add(dto);
        }

        return result;
    }

    private UserShortDto getInitiator(Long initiatorId) {
        if (initiatorId == null) {
            return null;
        }
        UserDto user = userClient.getUser(initiatorId);
        return new UserShortDto(user.getId(), user.getName());
    }

    /**
     * Один запрос к user-service на весь список событий вместо N запросов (проблема N+1).
     */
    private Map<Long, UserShortDto> getInitiatorsByUserId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        List<UserDto> users = userClient.getUsers(initiatorIds);

        Map<Long, UserShortDto> result = new HashMap<>();
        for (UserDto user : users) {
            result.put(user.getId(), new UserShortDto(user.getId(), user.getName()));
        }

        return result;
    }

    /**
     * Аналогично — один батч-запрос к main-service (категории) на весь список событий.
     */
    private Map<Long, CategoryDto> getCategoriesByCategoryId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .distinct()
                .toList();

        List<CategoryDto> categories = categoryClient.getCategories(categoryIds);

        Map<Long, CategoryDto> result = new HashMap<>();
        for (CategoryDto category : categories) {
            result.put(category.getId(), category);
        }

        return result;
    }

    private Map<Long, Long> getConfirmedRequestsByEventId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = getEventIds(events);

        List<EventConfirmedRequestsCountDto> counts = requestClient.getConfirmedCounts(eventIds);

        Map<Long, Long> result = new HashMap<>();
        for (EventConfirmedRequestsCountDto count : counts) {
            result.put(count.getEventId(), count.getConfirmedRequests());
        }

        return result;
    }

    private Long getViews(Event event) {
        if (event.getPublishedOn() == null) {
            return null;
        }

        String uri = getEventUri(event);

        GetStatsParams params = GetStatsParams.builder()
                .start(event.getPublishedOn())
                .end(LocalDateTime.now())
                .uris(List.of(uri))
                .unique(UNIQUE_VIEWS)
                .build();

        Map<String, Long> viewsByUri = statService.getViews(params);

        if (viewsByUri == null) {
            return null;
        }

        return viewsByUri.getOrDefault(uri, 0L);
    }

    private Map<Long, Long> getViewsByEventId(List<Event> events) {
        List<Event> eventsWithPublishedOn = getEventsWithPublishedOn(events);

        if (eventsWithPublishedOn.isEmpty()) {
            return Map.of();
        }

        GetStatsParams params = GetStatsParams.builder()
                .start(getMinPublishedOn(eventsWithPublishedOn))
                .end(LocalDateTime.now())
                .uris(getEventUris(eventsWithPublishedOn))
                .unique(UNIQUE_VIEWS)
                .build();

        Map<String, Long> viewsByUri = statService.getViews(params);

        if (viewsByUri == null) {
            return null;
        }

        Map<Long, Long> viewsByEventId = new HashMap<>();

        for (Event event : eventsWithPublishedOn) {
            String uri = getEventUri(event);
            Long views = viewsByUri.getOrDefault(uri, 0L);

            viewsByEventId.put(event.getId(), views);
        }

        return viewsByEventId;
    }

    private Long getViewsForEvent(Event event, Map<Long, Long> viewsByEventId) {
        if (viewsByEventId == null) {
            return null;
        }

        return viewsByEventId.get(event.getId());
    }

    private List<Event> getEventsWithPublishedOn(List<Event> events) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (event.getPublishedOn() != null) {
                result.add(event);
            }
        }

        return result;
    }

    private List<Long> getEventIds(List<Event> events) {
        Set<Long> uniqueIds = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueIds.add(event.getId());
        }

        return new ArrayList<>(uniqueIds);
    }

    private List<String> getEventUris(List<Event> events) {
        Set<String> uniqueUris = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueUris.add(getEventUri(event));
        }

        return new ArrayList<>(uniqueUris);
    }

    private LocalDateTime getMinPublishedOn(List<Event> events) {
        LocalDateTime minPublishedOn = events.get(0).getPublishedOn();

        for (Event event : events) {
            if (event.getPublishedOn().isBefore(minPublishedOn)) {
                minPublishedOn = event.getPublishedOn();
            }
        }

        return minPublishedOn;
    }

    private String getEventUri(Event event) {
        return "/events/" + event.getId();
    }
}