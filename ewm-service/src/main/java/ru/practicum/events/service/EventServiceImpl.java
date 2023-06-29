package ru.practicum.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.categories.model.Category;
import ru.practicum.events.dto.*;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.location.model.Location;
import ru.practicum.location.service.LocationService;
import ru.practicum.participation_request.model.Request;
import ru.practicum.users.model.User;
import ru.practicum.utils.FindEntityUtilService;
import ru.practicum.utils.PageableUtil;
import ru.practicum.utils.enums.EventState;
import ru.practicum.utils.enums.StateAction;
import ru.practicum.utils.mapper.EventMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final FindEntityUtilService findEntity;
    private final EventRepository eventRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public EventOutputFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = findEntity.findEventOrElseThrow(eventId);

        if (dto.getStateAction() != null) adminUpdateEventStatus(event, dto.getStateAction());

        Event updateEvent = updateEvent(event, dto);

        List<Request> confRequests = findEntity.findEventRequests(event);

        return EventMapper.toOutputDto(updateEvent, confRequests);
    }

    @Override
    public List<EventOutputFullDto> adminFindEvents(List<Long> users, List<EventState> states,
                                                    List<Long> categories, LocalDateTime rangeStart,
                                                    LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageableUtil.pageManager(from, size, null);

        List<Event> events = eventRepository.adminFindEvents(users, states, categories,
                                                            rangeStart, rangeEnd, pageable);

        Map<Event, List<Request>> confRequests = new HashMap<>();

        for (Event event : events) {
            List<Request> requests = findEntity.findEventRequests(event);
            confRequests.put(event, requests);
        }

        return EventMapper.toEventFullDtoList(events, confRequests);
    }

    @Override
    public List<EventShortDto> initiatorGetEvents(Long userId, int from, int size) {
        Pageable pageable = PageableUtil.pageManager(from, size, null);
        User initiator = findEntity.findUserOrElseThrow(userId);

        List<Event> events = eventRepository.findAllByInitiator(initiator, pageable);

        Map<Event, List<Request>> confRequests = new HashMap<>();

        for (Event event : events) {
            List<Request> requests = findEntity.findEventRequests(event);
            confRequests.put(event, requests);
        }

        return EventMapper.toEventShortList(events, confRequests);
    }

    @Override
    @Transactional
    public EventOutputFullDto initiatorAddEvent(Long userId, EventInputDto requestDto) {
        User initiator = findEntity.findUserOrElseThrow(userId);
        Category cat = findEntity.findCategoryOrElseThrow(requestDto.getCategoryId());
        Location loc = locationService.getLocationOrElseSave(requestDto.getLocation());

        Event event = EventMapper.toEvent(requestDto, initiator, cat, loc);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        event = eventRepository.save(event);

        return EventMapper.toOutputDto(event, List.of());
    }

    @Override
    public EventOutputFullDto initiatorGetEvent(Long userId, Long eventId) {
        findEntity.findUserOrElseThrow(userId);
        Event event = findEntity.findEventOrElseThrow(eventId);
        findEntity.checkEventInitiator(event, userId);

        List<Request> confRequests = findEntity.findEventRequests(event);

        return EventMapper.toOutputDto(event, confRequests);
    }

    @Override
    @Transactional
    public EventOutputFullDto initiatorUpdateEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {

        Event event = findEntity.findEventOrElseThrow(eventId);

        findEntity.checkEventInitiator(event, userId);
        findEntity.checkUnpublishedEvent(event);

        if (dto.getStateAction() != null) userUpdateEventStatus(event, dto.getStateAction());

        Event updateEvent = updateEvent(event, dto);
        List<Request> confRequests = findEntity.findEventRequests(updateEvent);

        return EventMapper.toOutputDto(updateEvent, confRequests);
    }

    @Override
    public List<EventOutputFullDto> findEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               String sort, int from, int size) {
        Pageable pageable = PageableUtil.pageManager(from, size, null);

        List<Event> events = eventRepository.findPublicEvents(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, pageable);

        Map<Event, List<Request>> confRequests = new HashMap<>();

        for (Event event : events) {
            List<Request> requests = findEntity.findEventRequests(event);
            confRequests.put(event, requests);
        }

        return EventMapper.toEventFullDtoList(events, confRequests);
    }

    @Override
    public EventOutputFullDto getEvent(Long id) {
        Event event = findEntity.findPublishedEventOrElseThrow(id);
        List<Request> confRequests = findEntity.findEventRequests(event);
        return EventMapper.toOutputDto(event, confRequests);
    }

    private void adminUpdateEventStatus(Event event, StateAction stateAction) {
        switch (stateAction) {
            case REJECT_EVENT:
                event.setState(EventState.CANCELED);
                break;
            case PUBLISH_EVENT:
                event.setState(EventState.PUBLISHED);
                break;
            default:
                findEntity.unsupportedStatus();
        }
    }

    private void userUpdateEventStatus(Event event, StateAction stateAction) {
        switch (stateAction) {
            case CANCEL_REVIEW:
                event.setState(EventState.CANCELED);
                break;
            case SEND_TO_REVIEW:
                event.setState(EventState.PENDING);
                break;
            default:
                findEntity.unsupportedStatus();
        }
    }

    private Event updateEvent(Event event, UpdateEventRequest dto) {
        if (dto.getAnnotation() != null && !dto.getAnnotation().isBlank()) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null)
        {
            Category cat = findEntity.findCategoryOrElseThrow(dto.getCategory());
            event.setCategory(cat);
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank())
            event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
        {
            Location loc = locationService.getLocationOrElseSave(dto.getLocation());
            event.setLocation(loc);
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) event.setTitle(dto.getTitle());

        return event;
    }

}
