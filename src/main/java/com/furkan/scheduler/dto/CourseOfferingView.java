package com.furkan.scheduler.dto;

import java.time.LocalTime;
import java.util.List;

public record CourseOfferingView(
        Long id,
        String courseCodeSec,
        String courseName,
        String instructor,
        int credits,
        int ects,
        List<MeetingView> meetings
){  public record MeetingView(
        String type,
        String day,
        LocalTime startTime,
        LocalTime endTime,
        String room
) {}
}