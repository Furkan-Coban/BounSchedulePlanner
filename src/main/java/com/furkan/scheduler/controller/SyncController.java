package com.furkan.scheduler.controller;

import com.furkan.scheduler.dto.SyncRequest;
import com.furkan.scheduler.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SyncController {
    private final SyncService syncService;

    public  SyncController(SyncService syncService){
        this.syncService = syncService;
    }
    @PostMapping("/sync")
    public ResponseEntity<?> sync(@Valid @RequestBody SyncRequest req) {
        return ResponseEntity.ok(syncService.syncDepartment(req.termCode(), req.deptCode(), req.deptName()));
    }
}
