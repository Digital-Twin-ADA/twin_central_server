package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.CurrentStageEventsDto;
import com.digitaltwin.central.dto.EventResponseDto;
import com.digitaltwin.central.dto.SpontaneousEventRequestDto;
import com.digitaltwin.central.model.LineupEvent;
import com.digitaltwin.central.model.SpontaneousEvent;
import com.digitaltwin.central.model.Stage;
import com.digitaltwin.central.repository.LineupEventRepository;
import com.digitaltwin.central.repository.SpontaneousEventRepository;
import com.digitaltwin.central.repository.StageRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@Tag(name = "Events", description = "Manage festival events: list, create, update, and stream live events. Use to display schedules, create spontaneous events, and keep clients updated.")
@RequestMapping("/api/events")
public class EventsController {

    private final LineupEventRepository lineupRepo;
    private final SpontaneousEventRepository spontaneousRepo;
    private final StageRepository stageRepo;
    private final SimpMessagingTemplate messaging;

    public EventsController(LineupEventRepository lineupRepo, SpontaneousEventRepository spontaneousRepo,
                            StageRepository stageRepo, SimpMessagingTemplate messaging) {
        this.lineupRepo = lineupRepo;
        this.spontaneousRepo = spontaneousRepo;
        this.stageRepo = stageRepo;
        this.messaging = messaging;
    }

    @GetMapping("/live")
    @Operation(summary = "Get live events", description = "Returns events that are currently in progress across all stages. Use for live displays or notifications to attendees and staff.")
    public List<EventResponseDto> getLiveEvents() {
        OffsetDateTime now = OffsetDateTime.now();
        List<EventResponseDto> result = new ArrayList<>();

        List<LineupEvent> lineup = lineupRepo.findAll();
        for (LineupEvent e : lineup) {
            if ((e.getStartsAt().isBefore(now) || e.getStartsAt().isEqual(now)) && e.getEndsAt().isAfter(now)) {
                result.add(new EventResponseDto(
                        e.getId(),
                        e.getArtist().getId(),
                        e.getArtist().getName(),
                        e.getArtist().getGenre(),
                        e.getStage().getId(),
                        e.getStage().getName(),
                        e.getStage().getZoneCode(),
                        e.getStartsAt(),
                        e.getEndsAt(),
                        e.getTitle(),
                        e.getStatus()
                ));
            }
        }

        List<SpontaneousEvent> spont = spontaneousRepo.findAllByOrderByStartsAtAsc();
        for (SpontaneousEvent s : spont) {
            if ((s.getStartsAt().isBefore(now) || s.getStartsAt().isEqual(now)) && s.getEndsAt().isAfter(now)) {
                Stage st = s.getStage();
                result.add(new EventResponseDto(
                        s.getId(),
                        s.getTitle(),
                        s.getDescription(),
                        st != null ? st.getId() : null,
                        st != null ? st.getName() : null,
                        st != null ? st.getZoneCode() : null,
                        s.getStartsAt(),
                        s.getEndsAt(),
                        s.getStatus()
                ));
            }
        }

        return result;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current events by stage", description = "Return currently running events grouped by stage. Use to build per-stage live views or dashboards.")
    public List<CurrentStageEventsDto> getCurrentEventsByStage() {
        OffsetDateTime now = OffsetDateTime.now();
        List<LineupEvent> currentLineup = lineupRepo.findByStartsAtLessThanEqualAndEndsAtGreaterThan(now, now);
        List<SpontaneousEvent> currentSpontaneous = spontaneousRepo.findByStartsAtLessThanEqualAndEndsAtGreaterThan(now, now);

        return stageRepo.findAllByOrderByIdAsc().stream()
                .map(stage -> {
                    List<EventResponseDto> events = new ArrayList<>();

                    for (LineupEvent e : currentLineup) {
                        if (e.getStage().getId().equals(stage.getId())) {
                            events.add(new EventResponseDto(
                                    e.getId(),
                                    e.getArtist().getId(),
                                    e.getArtist().getName(),
                                    e.getArtist().getGenre(),
                                    e.getStage().getId(),
                                    e.getStage().getName(),
                                    e.getStage().getZoneCode(),
                                    e.getStartsAt(),
                                    e.getEndsAt(),
                                    e.getTitle(),
                                    e.getStatus()
                            ));
                        }
                    }

                    for (SpontaneousEvent s : currentSpontaneous) {
                        Stage st = s.getStage();
                        if (st != null && st.getId().equals(stage.getId())) {
                            events.add(new EventResponseDto(
                                    s.getId(),
                                    s.getTitle(),
                                    s.getDescription(),
                                    st.getId(),
                                    st.getName(),
                                    st.getZoneCode(),
                                    s.getStartsAt(),
                                    s.getEndsAt(),
                                    s.getStatus()
                            ));
                        }
                    }

                    return new CurrentStageEventsDto(stage.getId(), stage.getName(), stage.getZoneCode(), events);
                })
                .toList();
    }

    @PostMapping("/spontaneous")
    @Operation(summary = "Create spontaneous event", description = "Create a one-off or ad-hoc event. Use when adding temporary shows or announcements; publishes the event to websocket topic /topic/events.")
    public ResponseEntity<?> createSpontaneous(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Spontaneous event payload") @RequestBody SpontaneousEventRequestDto req) {
        Stage stage = null;
        if (req.getStageId() != null) {
            Optional<Stage> opt = stageRepo.findById(req.getStageId());
            if (opt.isEmpty()) return ResponseEntity.badRequest().body("Stage not found with id: " + req.getStageId());
            stage = opt.get();
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime starts = req.getStartsAt() != null ? req.getStartsAt() : now;
        OffsetDateTime ends = req.getEndsAt() != null ? req.getEndsAt() : now.plusHours(1);

        SpontaneousEvent s = new SpontaneousEvent();
        s.setTitle(req.getTitle());
        s.setDescription(req.getDescription());
        s.setStage(stage);
        s.setStartsAt(starts);
        s.setEndsAt(ends);
        s.setCreatedAt(now);
        s.setStatus("LIVE");

        // validation: starts before ends
        if (!s.getStartsAt().isBefore(s.getEndsAt())) {
            return ResponseEntity.badRequest().body("startsAt must be before endsAt");
        }

        SpontaneousEvent saved = spontaneousRepo.save(s);

        EventResponseDto resp = new EventResponseDto(
                saved.getId(), saved.getTitle(), saved.getDescription(),
                stage != null ? stage.getId() : null,
                stage != null ? stage.getName() : null,
                stage != null ? stage.getZoneCode() : null,
                saved.getStartsAt(), saved.getEndsAt(), saved.getStatus()
        );

        // publish to websocket topic
        messaging.convertAndSend("/topic/events", (Object) resp);

        return ResponseEntity.created(URI.create("/api/events/spontaneous/" + saved.getId())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Fetch a lineup or spontaneous event by its ID. Use to display detailed event information.")
    public ResponseEntity<?> getEventById(@Parameter(description = "Event ID") @PathVariable Long id) {
        // Try lineup events first
        Optional<LineupEvent> leOpt = lineupRepo.findById(id);
        if (leOpt.isPresent()) {
            LineupEvent e = leOpt.get();
            EventResponseDto dto = new EventResponseDto(
                    e.getId(),
                    e.getArtist().getId(),
                    e.getArtist().getName(),
                    e.getArtist().getGenre(),
                    e.getStage().getId(),
                    e.getStage().getName(),
                    e.getStage().getZoneCode(),
                    e.getStartsAt(),
                    e.getEndsAt(),
                    e.getTitle(),
                    e.getStatus()
            );
            return ResponseEntity.ok(dto);
        }

        // Then spontaneous events
        Optional<SpontaneousEvent> seOpt = spontaneousRepo.findById(id);
        if (seOpt.isPresent()) {
            SpontaneousEvent s = seOpt.get();
            Stage st = s.getStage();
            EventResponseDto dto = new EventResponseDto(
                    s.getId(),
                    s.getTitle(),
                    s.getDescription(),
                    st != null ? st.getId() : null,
                    st != null ? st.getName() : null,
                    st != null ? st.getZoneCode() : null,
                    s.getStartsAt(),
                    s.getEndsAt(),
                    s.getStatus()
            );
            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/spontaneous/{id}")
    @Operation(summary = "Update spontaneous event", description = "Replace an existing spontaneous event. Use for full updates to title, description, times, stage, or status.")
    public ResponseEntity<?> updateSpontaneous(@Parameter(description = "ID of the spontaneous event") @PathVariable Long id, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Spontaneous event update payload") @RequestBody SpontaneousEventRequestDto req) {
        Optional<SpontaneousEvent> seOpt = spontaneousRepo.findById(id);
        if (seOpt.isEmpty()) return ResponseEntity.notFound().build();
        SpontaneousEvent s = seOpt.get();

        Stage stage = null;
        if (req.getStageId() != null) {
            Optional<Stage> opt = stageRepo.findById(req.getStageId());
            if (opt.isEmpty()) return ResponseEntity.badRequest().body("Stage not found with id: " + req.getStageId());
            stage = opt.get();
        }

        s.setTitle(req.getTitle());
        s.setDescription(req.getDescription());
        s.setStage(stage);
        s.setStartsAt(req.getStartsAt() != null ? req.getStartsAt() : s.getStartsAt());
        s.setEndsAt(req.getEndsAt() != null ? req.getEndsAt() : s.getEndsAt());

        // validation: starts before ends
        if (s.getStartsAt() != null && s.getEndsAt() != null && !s.getStartsAt().isBefore(s.getEndsAt())) {
            return ResponseEntity.badRequest().body("startsAt must be before endsAt");
        }

        SpontaneousEvent saved = spontaneousRepo.save(s);
        Stage st = saved.getStage();
        EventResponseDto dto = new EventResponseDto(
                saved.getId(), saved.getTitle(), saved.getDescription(),
                st != null ? st.getId() : null,
                st != null ? st.getName() : null,
                st != null ? st.getZoneCode() : null,
                saved.getStartsAt(), saved.getEndsAt(), saved.getStatus()
        );
        messaging.convertAndSend("/topic/events", (Object) dto);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/spontaneous/{id}")
    @Operation(summary = "Patch spontaneous event", description = "Partially update fields of a spontaneous event. Use for small edits without replacing the whole resource.")
    public ResponseEntity<?> patchSpontaneous(@Parameter(description = "ID of spontaneous event") @PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Optional<SpontaneousEvent> seOpt = spontaneousRepo.findById(id);
        if (seOpt.isEmpty()) return ResponseEntity.notFound().build();
        SpontaneousEvent s = seOpt.get();

        if (updates.containsKey("title")) s.setTitle((String) updates.get("title"));
        if (updates.containsKey("description")) s.setDescription((String) updates.get("description"));
        if (updates.containsKey("startsAt") && updates.get("startsAt") != null) s.setStartsAt(OffsetDateTime.parse((String) updates.get("startsAt")));
        if (updates.containsKey("endsAt") && updates.get("endsAt") != null) s.setEndsAt(OffsetDateTime.parse((String) updates.get("endsAt")));

        // validation: starts before ends if both present
        if (s.getStartsAt() != null && s.getEndsAt() != null && !s.getStartsAt().isBefore(s.getEndsAt())) {
            return ResponseEntity.badRequest().body("startsAt must be before endsAt");
        }
        if (updates.containsKey("stageId")) {
            Object v = updates.get("stageId");
            if (v == null) {
                s.setStage(null);
            } else {
                Long sid = v instanceof Number ? ((Number) v).longValue() : Long.valueOf(v.toString());
                Optional<Stage> opt = stageRepo.findById(sid);
                if (opt.isEmpty()) return ResponseEntity.badRequest().body("Stage not found with id: " + sid);
                s.setStage(opt.get());
            }
        }
        if (updates.containsKey("status")) s.setStatus((String) updates.get("status"));

        SpontaneousEvent saved = spontaneousRepo.save(s);
        Stage st = saved.getStage();
        EventResponseDto dto = new EventResponseDto(
                saved.getId(), saved.getTitle(), saved.getDescription(),
                st != null ? st.getId() : null,
                st != null ? st.getName() : null,
                st != null ? st.getZoneCode() : null,
                saved.getStartsAt(), saved.getEndsAt(), saved.getStatus()
        );
        messaging.convertAndSend("/topic/events", (Object) dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/spontaneous/{id}")
    @Operation(summary = "Delete spontaneous event", description = "Delete a spontaneous event by ID. Use when cancelling or removing an ad-hoc event; notifies websocket subscribers.")
    public ResponseEntity<?> deleteSpontaneous(@Parameter(description = "ID of spontaneous event to delete") @PathVariable Long id) {
        Optional<SpontaneousEvent> seOpt = spontaneousRepo.findById(id);
        if (seOpt.isEmpty()) return ResponseEntity.notFound().build();
        spontaneousRepo.deleteById(id);
        messaging.convertAndSend("/topic/events", (Object) java.util.Map.of("action", "deleted", "type", "spontaneous", "id", id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List all events", description = "List both lineup and spontaneous events ordered by start time. Use for full schedule views.")
    public List<EventResponseDto> listAll() {
        List<EventResponseDto> out = new ArrayList<>();
        for (LineupEvent e : lineupRepo.findAllByOrderByStartsAtAsc()) {
            out.add(new EventResponseDto(
                    e.getId(),
                    e.getArtist().getId(),
                    e.getArtist().getName(),
                    e.getArtist().getGenre(),
                    e.getStage().getId(),
                    e.getStage().getName(),
                    e.getStage().getZoneCode(),
                    e.getStartsAt(),
                    e.getEndsAt(),
                    e.getTitle(),
                    e.getStatus()
            ));
        }
        for (SpontaneousEvent s : spontaneousRepo.findAllByOrderByStartsAtAsc()) {
            Stage st = s.getStage();
            out.add(new EventResponseDto(
                    s.getId(), s.getTitle(), s.getDescription(),
                    st != null ? st.getId() : null,
                    st != null ? st.getName() : null,
                    st != null ? st.getZoneCode() : null,
                    s.getStartsAt(), s.getEndsAt(), s.getStatus()
            ));
        }
        return out;
    }

    @GetMapping("/spontaneous")
    @Operation(summary = "List spontaneous events", description = "Return paginated spontaneous events with optional filtering by stage, status, or time range. Use for admin listing and APIs.")
    public ResponseEntity<?> listSpontaneous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        List<SpontaneousEvent> all = spontaneousRepo.findAllByOrderByStartsAtAsc();

        final OffsetDateTime fromDtParsed;
        final OffsetDateTime toDtParsed;
        try {
            fromDtParsed = (from != null) ? OffsetDateTime.parse(from) : null;
            toDtParsed = (to != null) ? OffsetDateTime.parse(to) : null;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid from/to datetime format. Use ISO_OFFSET_DATE_TIME");
        }

        List<EventResponseDto> filtered = all.stream()
                .filter(s -> stageId == null || (s.getStage() != null && s.getStage().getId().equals(stageId)))
                .filter(s -> status == null || (s.getStatus() != null && s.getStatus().equalsIgnoreCase(status)))
                .filter(s -> fromDtParsed == null || !s.getEndsAt().isBefore(fromDtParsed))
                .filter(s -> toDtParsed == null || !s.getStartsAt().isAfter(toDtParsed))
                .map(s -> {
                    Stage st = s.getStage();
                    return new EventResponseDto(
                            s.getId(), s.getTitle(), s.getDescription(),
                            st != null ? st.getId() : null,
                            st != null ? st.getName() : null,
                            st != null ? st.getZoneCode() : null,
                            s.getStartsAt(), s.getEndsAt(), s.getStatus()
                    );
                })
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        if (start > filtered.size()) {
            start = end = 0;
        }
        List<EventResponseDto> pageContent = filtered.subList(start, end);
        PageImpl<EventResponseDto> dtoPage = new PageImpl<>(pageContent, PageRequest.of(page, size, Sort.by("startsAt").ascending()), filtered.size());
        return ResponseEntity.ok(dtoPage);
    }
}
