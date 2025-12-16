package com.furkan.scheduler.controller;

import com.furkan.scheduler.dto.DepartmentSeed;
import com.furkan.scheduler.ingest.DepartmentCatalogParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DepartmentCatalogController {

    private final DepartmentCatalogParser parser;

    public DepartmentCatalogController(DepartmentCatalogParser parser) {
        this.parser = parser;
    }

    @GetMapping("/dept-catalog/from-resource")
    public List<DepartmentSeed> fromResource() throws Exception {
        var res = new ClassPathResource("semester.html");

        // Read file bytes and decode as UTF-8 (HTML is mostly ASCII; this is fine)
        String html = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return parser.parse(html);
    }
}
