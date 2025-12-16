package com.furkan.scheduler.service;



import com.furkan.scheduler.domain.CourseOffering;

import com.furkan.scheduler.domain.Department;

import com.furkan.scheduler.domain.Meeting;

import com.furkan.scheduler.domain.Term;

import com.furkan.scheduler.dto.CourseOfferingDto;

import com.furkan.scheduler.dto.MeetingDto;

import com.furkan.scheduler.ingest.BounSchedulerParser;

import com.furkan.scheduler.ingest.ScheduleFetcher;

import com.furkan.scheduler.ingest.TimeSlotMapper;

import com.furkan.scheduler.repo.CourseOfferingRepository;

import com.furkan.scheduler.repo.DepartmentRepository;

import com.furkan.scheduler.repo.MeetingRepository;

import com.furkan.scheduler.repo.TermRepository;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.util.List;



@Service

public class SyncService {



    private final TermRepository termRepo;

    private final DepartmentRepository deptRepo;

    private final CourseOfferingRepository offeringRepo;

    private final MeetingRepository meetingRepo;



    private final ScheduleFetcher fetcher;

    private final BounSchedulerParser parser;

    private final TimeSlotMapper slotMapper;



    public SyncService(

            TermRepository termRepo,

            DepartmentRepository deptRepo,

            CourseOfferingRepository offeringRepo,

            MeetingRepository meetingRepo,

            ScheduleFetcher fetcher,

            BounSchedulerParser parser,

            TimeSlotMapper slotMapper

    ) {

        this.termRepo = termRepo;

        this.deptRepo = deptRepo;

        this.offeringRepo = offeringRepo;

        this.meetingRepo = meetingRepo;

        this.fetcher = fetcher;

        this.parser = parser;

        this.slotMapper = slotMapper;

    }



    @Transactional

    public SyncResult syncDepartment(String termCode, String deptCode, String deptName) {



        Term term = termRepo.findByCode(termCode)

                .orElseGet(() -> termRepo.save(new Term(null, termCode)));



        Department dept = deptRepo.findByCode(deptCode)

                .orElseGet(() -> deptRepo.save(new Department(null, deptCode, deptName)));



        String html = fetcher.fetchHtml(termCode, deptCode, deptName);

        List<CourseOfferingDto> dtos = parser.parseDepartmentSchedule(html);



        int created = 0, updated = 0, meetingsReplaced = 0;



        for (CourseOfferingDto dto : dtos) {



            CourseOffering offering = offeringRepo

                    .findByTerm_CodeAndDepartment_CodeAndCourseCodeSec(termCode, deptCode, dto.codeSec())

                    .orElseGet(() -> {

                        CourseOffering o = new CourseOffering();

                        o.setTerm(term);

                        o.setDepartment(dept);

                        o.setCourseCodeSec(dto.codeSec());

                        return o;

                    });



            boolean isNew = (offering.getId() == null);



            // scalar fields

            offering.setTerm(term);

            offering.setCourseName(dto.name());         // entity uses courseName

            offering.setCredits(dto.credits());

            offering.setEcts(dto.ects());

            offering.setInstructor(dto.instructor());





            offering = offeringRepo.save(offering);



            if (isNew) created++; else updated++;



            // replace meetings atomically for this offering

            meetingRepo.deleteByOffering_Id(offering.getId());



            offeringRepo.save(offering);

        if (isNew) created++; else updated++;

        // replace meetings atomically for this offering
        meetingRepo.deleteByOffering_Id(offering.getId());
        
        // --- START NEW/IMPROVED LOGIC ---
        List<Meeting> newMeetings = new java.util.ArrayList<>();
        for (MeetingDto m : dto.meetings()) {
            var tr = slotMapper.toTimeRange(m.slot());

            Meeting meeting = new Meeting();
            meeting.setOffering(offering);  // your mapping name
            meeting.setDay(m.day());
            meeting.setStartTime(tr.start());
            meeting.setEndTime(tr.end());
            meeting.setRoom(m.room());
            meeting.setType(m.type());
            newMeetings.add(meeting);
        }
        
        // Save all meetings for this offering in one batch
        if (!newMeetings.isEmpty()) {
            meetingRepo.saveAll(newMeetings);
            meetingsReplaced += newMeetings.size();
        }
        // --- END NEW/IMPROVED LOGIC ---
        
        // Optional but recommended: Flush the session to ensure data is written 
        // before the transaction ends and the AdminController loop continues.
        // This is often not needed with @Transactional but helps guarantee visibility 
        // in rapid sequential operations like this.
        offeringRepo.flush(); 
        }



        return new SyncResult(termCode, deptCode, deptName, dtos.size(), created, updated, meetingsReplaced);

    }



    public record SyncResult(

            String term,

            String deptCode,

            String deptName,

            int parsedOfferings,

            int offeringsCreated,

            int offeringsUpdated,

            int meetingsReplaced

    ) {}

}