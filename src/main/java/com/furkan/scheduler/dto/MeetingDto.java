package com.furkan.scheduler.dto;

public record MeetingDto(
     String type,     // "LEC" | "PS" | "LAB" (derived from row)
     String day,      // "M","T","W","Th","F" (normalize)
     int slot,        // 1..n (store slot; convert to LocalTime later)
     String room      // e.g. "KB Z01" / "NH 401"
){}
