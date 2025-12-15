
package com.furkan.scheduler.ingest;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

@Component
public class TimeSlotMapper {

    private static final LocalTime BASE_START = LocalTime.of(9, 0);   // slot 1
    private static final Duration SLOT_STEP = Duration.ofMinutes(60); // 1 hour steps
    private static final Duration SLOT_LEN  = Duration.ofMinutes(50); // 50 min lecture

    public TimeRange toTimeRange(int slot) {
        if (slot <= 0) throw new IllegalArgumentException("slot must be >= 1. Got: " + slot);

        LocalTime start = BASE_START.plus(SLOT_STEP.multipliedBy(slot - 1L));
        LocalTime end = start.plus(SLOT_LEN);
        return new TimeRange(start, end);
    }
    // ✅ NEW: startTime -> slot
    public int toSlot(LocalTime startTime) {
        long minutes = Duration.between(BASE_START, startTime).toMinutes();
        if (minutes < 0 || minutes % 60 != 0) return -1;
        return (int) (minutes / 60) + 1;
    }
    public record TimeRange(LocalTime start, LocalTime end) {}
}
