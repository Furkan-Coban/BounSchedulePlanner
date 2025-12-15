package com.furkan.scheduler.dto;

public record CourseCardDto(
        Long id,
        String departmentCode,
        String courseCodeSec,
        String courseName,
        String instructor,
        int credits,
        int ects,
        String daysText,
        String hoursText,
        String roomsText
) {}
