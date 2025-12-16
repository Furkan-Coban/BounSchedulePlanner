package com.furkan.scheduler.controller;

import com.furkan.scheduler.domain.CourseOffering;
import com.furkan.scheduler.domain.Meeting;
import com.furkan.scheduler.dto.CourseOfferingView;
import com.furkan.scheduler.repo.CourseOfferingRepository;
import com.furkan.scheduler.repo.MeetingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class CourseController {
    private final CourseOfferingRepository offeringRepo;
    private final MeetingRepository meetingRepo;



public CourseController(CourseOfferingRepository offeringRepo, MeetingRepository meetingRepo) {
    this.offeringRepo = offeringRepo;
    this.meetingRepo = meetingRepo;

}   @GetMapping("/courses")
    public List<CourseOfferingView> search(
            @RequestParam String term,
            @RequestParam String q
    ) {
        // simple: search by codeSec first (works well for CMPE150)
        List<CourseOffering> offerings =
                offeringRepo.findTop200ByTerm_CodeAndCourseCodeSecContainingIgnoreCaseOrderByCourseCodeSecAsc(term, q);

        if (offerings.isEmpty()) {
            // fallback: search by name
            offerings =
                    offeringRepo.findTop200ByTerm_CodeAndCourseNameContainingIgnoreCaseOrderByCourseCodeSecAsc(term, q);
        }

        List<Long> ids = offerings.stream().map(CourseOffering::getId).toList();
        List<Meeting> meetings = ids.isEmpty() ? List.of() : meetingRepo.findByOffering_IdIn(ids);

        Map<Long, List<Meeting>> byOfferingId = meetings.stream()
                .collect(Collectors.groupingBy(m -> m.getOffering().getId()));

        List<CourseOfferingView> out = new ArrayList<>();
        for (CourseOffering o : offerings) {
            List<CourseOfferingView.MeetingView> mviews = byOfferingId
                    .getOrDefault(o.getId(), List.of())
                    .stream()
                    .map(m -> new CourseOfferingView.MeetingView(
                            m.getType(), m.getDay(), m.getStartTime(), m.getEndTime(), m.getRoom()
                    ))
                    .toList();

            out.add(new CourseOfferingView(
                    o.getId(),
                    o.getCourseCodeSec(),
                    o.getCourseName(),
                    o.getInstructor(),
                    o.getCredits(),
                    o.getEcts(),
                    mviews
            ));
        }
        return out;
    }
}