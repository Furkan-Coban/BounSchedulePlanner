package com.furkan.scheduler.controller;

import com.furkan.scheduler.dto.SyncRequest;
import com.furkan.scheduler.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    public SyncService.SyncResult sync(
            @RequestParam String term,
            @RequestParam String dept,
            @RequestParam String deptName
    ) {
        return syncService.syncDepartment(term, dept, deptName);
    }
}