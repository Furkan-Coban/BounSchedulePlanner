package com.furkan.scheduler.controller;

import com.furkan.scheduler.domain.CourseOffering;
import com.furkan.scheduler.domain.Meeting;
import com.furkan.scheduler.dto.CourseCardDto;
import com.furkan.scheduler.ingest.TimeSlotMapper;
import com.furkan.scheduler.repo.CourseOfferingRepository;
import com.furkan.scheduler.repo.MeetingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CourseCardsController {

    private final CourseOfferingRepository offeringRepo;
    private final MeetingRepository meetingRepo;
    private final TimeSlotMapper slotMapper;

    public CourseCardsController(
            CourseOfferingRepository offeringRepo,
            MeetingRepository meetingRepo,
            TimeSlotMapper slotMapper
    ) {
        this.offeringRepo = offeringRepo;
        this.meetingRepo = meetingRepo;
        this.slotMapper = slotMapper;
    }

    @GetMapping("/courses/cards")
    public List<CourseCardDto> cards(
            @RequestParam String term,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) String q
    ) {
        List<CourseOffering> offerings;

        if (q != null && !q.isBlank()) {
            // Search by code first; if empty fallback to name (fast and good enough)
            offerings = offeringRepo.findTop200ByTerm_CodeAndCourseCodeSecContainingIgnoreCaseOrderByCourseCodeSecAsc(term, q);
            if (offerings.isEmpty()) {
                offerings = offeringRepo.findTop200ByTerm_CodeAndCourseNameContainingIgnoreCaseOrderByCourseCodeSecAsc(term, q);
            }
        } else if (dept != null && !dept.isBlank()) {
            offerings = offeringRepo.findByTerm_CodeAndDepartment_CodeOrderByCourseCodeSecAsc(term, dept);
            // Optional: cap to 200
            if (offerings.size() > 200) offerings = offerings.subList(0, 200);
        } else {
            // No filters → don’t dump the whole term here
            return List.of();
        }

        List<Long> ids = offerings.stream().map(CourseOffering::getId).toList();
        List<Meeting> meetings = ids.isEmpty() ? List.of() : meetingRepo.findByOffering_IdIn(ids);

        Map<Long, List<Meeting>> byOfferingId = meetings.stream()
                .collect(Collectors.groupingBy(m -> m.getOffering().getId()));

        List<CourseCardDto> out = new ArrayList<>(offerings.size());

        for (CourseOffering o : offerings) {
            List<Meeting> ms = byOfferingId.getOrDefault(o.getId(), List.of());

            // Sort meetings by day then start
            ms = ms.stream()
                    .sorted(Comparator
                            .comparing((Meeting m) -> dayOrder(m.getDay()))
                            .thenComparing(Meeting::getStartTime))
                    .toList();

            String daysText = ms.stream().map(Meeting::getDay).collect(Collectors.joining(""));
            String hoursText = ms.stream()
                    .map(m -> slotMapper.toSlot(m.getStartTime()))
                    .filter(s -> s > 0)
                    .map(String::valueOf)
                    .collect(Collectors.joining(""));

            // Rooms: unique but keep order
            String roomsText = ms.stream()
                    .map(Meeting::getRoom)
                    .filter(r -> r != null && !r.isBlank())
                    .distinct()
                    .collect(Collectors.joining(" | "));

            out.add(new CourseCardDto(
                    o.getId(),
                    o.getDepartment().getCode(),
                    o.getCourseCodeSec(),
                    o.getCourseName(),
                    o.getInstructor(),
                    o.getCredits(),
                    o.getEcts(),
                    daysText,
                    hoursText,
                    roomsText
            ));
        }

        return out;
    }

    private static int dayOrder(String d) {
        // M T W Th F (you store "Th" as two chars)
        return switch (d) {
            case "M" -> 1;
            case "T" -> 2;
            case "W" -> 3;
            case "Th" -> 4;
            case "F" -> 5;
            default -> 99;
        };
    }
}
