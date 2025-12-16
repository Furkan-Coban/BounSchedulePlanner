package com.furkan.scheduler.dto;

public record CourseOfferingDto(
        String codeSec,          // "CHEM101.02"
        String courseCode,       // "CHEM101"
        String section,          // "02"
        String name,             // "GENERAL CHEMISTRY I"
        int credits,             // 3
        int ects,                // 6
        String instructor,       // "ŞARON ÇATAK"
        String deliveryMethod,   // empty / "Online" etc. (keep raw)
        String examDate,         // raw like "02.01.2026" (parse later if you want)
        Integer sl,              // nullable
        String requiredForDept,  // raw like "CHEM" / "" (keep raw)
        String departments,      // raw list like "ME; CMPE; EE; PHYS"
        java.util.List<MeetingDto> meetings
) {}