package com.furkan.scheduler.dto;

public record DepartmentSeed(
        String code,     // kisaadi
        String bolum,    // bolum query param (long name expected by sch.asp)
        String label     // visible text on the semester page (optional)
) {}
