package com.furkan.scheduler.controller;

import com.furkan.scheduler.ingest.DeptCatalog;
import com.furkan.scheduler.service.SyncService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminSyncController {

    private final SyncService syncService;

    public AdminSyncController(SyncService syncService) {
        this.syncService = syncService;
    }
    private static void sleepQuietly(long ms) {
    try {
        Thread.sleep(ms);
    } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
    }
}


    @PostMapping("/sync/term")
    public Map<String, Object> syncAllDeptsForTerm(@RequestParam String term) {
        

        int ok = 0;
        int fail = 0;
        List<Map<String, String>> errors = new ArrayList<>();

        for (DeptCatalog.Dept d : DeptCatalog.ALL) {
            try {
                syncService.syncDepartment(term, d.code(), d.name());
                ok++;
            } catch (Exception e) {
                fail++;
                errors.add(Map.of(
                        "dept", d.code(),
                        "name", d.name(),
                        "error", safeMsg(e)
                ));
            }
            sleepQuietly(250);
        }

        return Map.of(
                "term", term,
                "departmentsTotal", DeptCatalog.ALL.size(),
                "syncedOk", ok,
                "syncedFail", fail,
                "errors", errors,
                "ts", Instant.now().toString()
        );
    }

    private static String safeMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 250 ? m.substring(0, 250) : m;
    }
}
