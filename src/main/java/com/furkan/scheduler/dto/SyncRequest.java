package com.furkan.scheduler.dto;

import jakarta.validation.constraints.NotBlank;

public record SyncRequest(
        @NotBlank String termCode,       // "2024/2025-1"
        @NotBlank String deptCode,       // "ME"
        @NotBlank String deptName        // "MECHANICAL ENGINEERING"
) {}